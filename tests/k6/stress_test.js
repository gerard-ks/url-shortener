import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate, Gauge } from 'k6/metrics';

//  MÉTRIQUES CUSTOM
const redirectLatency = new Trend('redirect_latency_ms');
const redirectErrors = new Counter('redirect_errors');
const successRate = new Rate('redirect_success_rate');
const concurrentUsers = new Gauge('concurrent_users');

//  CONFIGURATION DU STRESS TEST
//  Objectif : Trouver le point de rupture (breaking point)
export const options = {
    stages: [
        // Phase 1 : Warm-up progressif
        { duration: '1m', target: 50 },     // 0 → 50 VUs

        // Phase 2 : Montée en charge agressive
        { duration: '2m', target: 200 },    // 50 → 200 VUs
        { duration: '2m', target: 500 },    // 200 → 500 VUs
        { duration: '2m', target: 1000 },   // 500 → 1000 VUs (stress élevé)

        // Phase 3 : Spike brutal (pic de trafic)
        { duration: '30s', target: 1500 },  // 1000 → 1500 VUs (spike)
        { duration: '1m', target: 1500 },   // Maintien du pic

        // Phase 4 : Descente progressive
        { duration: '1m', target: 500 },    // 1500 → 500 VUs
        { duration: '1m', target: 100 },    // 500 → 100 VUs
        { duration: '30s', target: 0 },     // Cool-down
    ],

    // Seuils plus souples (c'est un stress test, on s'attend à des échecs)
    thresholds: {
        'http_req_duration': ['p(95)<500'],           // P95 < 500ms (dégradé acceptable)
        'http_req_duration{expected_response:true}': ['p(99)<1000'],
        'redirect_success_rate': ['rate>0.95'],       // 95% de succès minimum
        'http_req_failed': ['rate<0.1'],              // Max 10% d'erreurs
    },
};

//  CONFIGURATION
const BASE_URL = __ENV.BASE_URL || 'http://10.204.70.251:8080';
const SHORT_CODE = __ENV.SHORT_CODE || 'stress-test';

//  SETUP : Créer une URL de test
export function setup() {
    console.log('STRESS TEST - Initialisation...');

    const createRes = http.post(
        `${BASE_URL}/api/v1/shorten`,
        JSON.stringify({
            url: 'https://www.example.com/stress-test-destination',
            custom_code: SHORT_CODE,
        }),
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );

    if (createRes.status === 201) {
        console.log(`URL de test créée : ${SHORT_CODE}`);
    } else if (createRes.status === 409) {
        console.log(`URL de test déjà existante : ${SHORT_CODE}`);
    } else {
        console.error(`Échec de la création : ${createRes.status}`);
    }

    return { shortCode: SHORT_CODE };
}


//  SCÉNARIO PRINCIPAL - STRESS TEST
export default function (data) {
    // Enregistrer le nombre d'utilisateurs concurrent
    concurrentUsers.add(__VU);

    const start = Date.now();

    const res = http.get(`${BASE_URL}/r/${data.shortCode}`, {
        redirects: 0,
        tags: { name: 'StressRedirect' },
        timeout: '10s', // Timeout plus long pour gérer la surcharge
    });

    const duration = Date.now() - start;

    // Enregistrer la latence
    redirectLatency.add(duration);

    // Vérifications
    const success = check(res, {
        'is status 302': (r) => r.status === 302,
        'has Location header': (r) => r.headers['Location'] !== undefined,
        'latency < 1s': (r) => duration < 1000,
    });

    successRate.add(success);

    if (!success) {
        redirectErrors.add(1);

        // Logger les erreurs détaillées
        if (res.status !== 302) {
            console.error(`[VU ${__VU}] Status: ${res.status}, Latency: ${duration}ms`);
        }
    }

    // Pause très courte (haute pression)
    sleep(0.05); // 50ms seulement
}

//  TEARDOWN : Rapport final
export function teardown(data) {
    console.log('\n' + '═'.repeat(70));
    console.log(' STRESS TEST TERMINÉ');
    console.log('═'.repeat(70));
    console.log(` URL testée : ${data.shortCode}`);
    console.log('═'.repeat(70) + '\n');
}


//  RAPPORT DÉTAILLÉ
export function handleSummary(data) {
    const metrics = data.metrics;

    // Extraire les métriques clés
    const p50 = metrics.redirect_latency_ms?.values['p(50)'] || 0;
    const p95 = metrics.redirect_latency_ms?.values['p(95)'] || 0;
    const p99 = metrics.redirect_latency_ms?.values['p(99)'] || 0;
    const max = metrics.redirect_latency_ms?.values.max || 0;

    const httpReqDuration = metrics.http_req_duration?.values || {};
    const httpP95 = httpReqDuration['p(95)'] || 0;

    const totalRequests = metrics.http_reqs?.values.count || 0;
    const failedRequests = metrics.http_req_failed?.values.passes || 0;
    const successRateValue = metrics.redirect_success_rate?.values.rate || 0;

    const errorCount = metrics.redirect_errors?.values.count || 0;

    // Construction du rapport console
    const report = `
${'═'.repeat(70)}
  RÉSULTATS DU STRESS TEST
${'═'.repeat(70)}

 LATENCE (redirect_latency_ms)
  • P50 (médiane)    : ${p50.toFixed(2)} ms
  • P95              : ${p95.toFixed(2)} ms
  • P99              : ${p99.toFixed(2)} ms
  • Max              : ${max.toFixed(2)} ms

 LATENCE HTTP (http_req_duration)
  • P95              : ${httpP95.toFixed(2)} ms

 REQUÊTES
  • Total            : ${totalRequests}
  • Échecs           : ${failedRequests}
  • Taux de succès   : ${(successRateValue * 100).toFixed(2)}%
  • Erreurs métier   : ${errorCount}

 VALIDATION OBJECTIFS STRESS
  • P95 < 500ms      : ${httpP95 < 500 ? ' VALIDÉ' : ' ÉCHOUÉ'} (${httpP95.toFixed(2)} ms)
  • Succès > 95%     : ${successRateValue > 0.95 ? ' VALIDÉ' : ' ÉCHOUÉ'} (${(successRateValue * 100).toFixed(2)}%)
  • Erreurs < 10%    : ${(failedRequests / totalRequests) < 0.1 ? ' VALIDÉ' : ' ÉCHOUÉ'}

${'═'.repeat(70)}

💡 RECOMMANDATIONS
${generateRecommendations(httpP95, successRateValue, errorCount)}

${'═'.repeat(70)}
`;

    console.log(report);

    // Sauvegarder les rapports
    return {
        '/results/stress-test-summary.json': JSON.stringify(data, null, 2),
        '/results/stress-test-report.txt': report,
        'stdout': report,
    };
}

//  GÉNÉRATION DES RECOMMANDATIONS
function generateRecommendations(p95, successRate, errors) {
    let recommendations = [];

    if (p95 > 500) {
        recommendations.push('  Latence P95 élevée : Augmentez le pool de connexions DB');
        recommendations.push('  Considérez l\'ajout de Redis pour cache distribué');
    }

    if (p95 > 200 && p95 <= 500) {
        recommendations.push('  Latence acceptable mais optimisable');
        recommendations.push('  Vérifiez les slow queries dans PostgreSQL');
    }

    if (successRate < 0.95) {
        recommendations.push('  Taux de succès < 95% : Risque de perte de données');
        recommendations.push('  Vérifiez les logs applicatifs pour identifier les erreurs');
    }

    if (errors > 100) {
        recommendations.push('  Nombre d\'erreurs élevé : Vérifiez la stabilité du système');
    }

    if (p95 < 200 && successRate > 0.99) {
        recommendations.push('  Performances excellentes sous stress !');
        recommendations.push('  Votre API supporte bien la charge');
    }

    if (recommendations.length === 0) {
        recommendations.push('  Aucune recommandation : système stable');
    }

    return recommendations.join('\n');
}
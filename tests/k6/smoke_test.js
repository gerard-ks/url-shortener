import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 1,
    duration: '30s',
};

const BASE_URL = __ENV.BASE_URL || 'http://10.204.70.251:8080';

export default function () {
    const res = http.get(`${BASE_URL}/actuator/health`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 200ms': (r) => r.timings.duration < 200,
    });
}
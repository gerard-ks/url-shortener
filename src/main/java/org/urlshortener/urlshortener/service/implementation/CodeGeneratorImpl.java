package org.urlshortener.urlshortener.service.implementation;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.urlshortener.urlshortener.service.interfaces.CodeGenerator;

import java.util.concurrent.ThreadLocalRandom;

import static org.urlshortener.urlshortener.constants.Constants.*;

@Service
@Slf4j
public class CodeGeneratorImpl implements CodeGenerator {

    @Override
    public String generate(int length) {
        StringBuilder code = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(BASE62_ALPHABET.length());
            code.append(BASE62_ALPHABET.charAt(index));
        }

        return code.toString();
    }
}

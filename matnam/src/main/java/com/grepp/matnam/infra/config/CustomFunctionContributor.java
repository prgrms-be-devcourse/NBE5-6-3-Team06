package com.grepp.matnam.infra.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Component;

@Component
public class CustomFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(final FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry()
            .registerPattern("match_against", "match (?1, ?2) against (?3 in boolean mode)",
                functionContributions.getTypeConfiguration()
                    .getBasicTypeRegistry()
                    .resolve(StandardBasicTypes.DOUBLE));
    }
}
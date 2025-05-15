package com.dpolach.inmemoryrepository.query;

import com.dpolach.inmemoryrepository.InMemoryEntityStore;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class StringBasedInMemoryQuery implements RepositoryQuery {

    private final String queryString;
    private final InMemoryQueryMethod method;
    private final InMemoryEntityStore entityStore;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Expression expression;

    public StringBasedInMemoryQuery(String queryString, InMemoryQueryMethod method,
                                    InMemoryEntityStore entityStore) {
        this.queryString = queryString;
        this.method = method;
        this.entityStore = entityStore;
        this.expression = parser.parseExpression(queryString);
    }

    @Override
    public Object execute(Object[] parameters) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        // Nastavení parametrů pro vyhodnocení výrazu
        for (int i = 0; i < parameters.length; i++) {
            context.setVariable("p" + i, parameters[i]);
        }

        // Pro zjednodušení předpokládáme, že výraz je predikát, který lze přímo použít pro filtrování
        // V reálném scénáři byste zde pravděpodobně museli implementovat složitější logiku

        if (method.isCollectionQuery()) {
            return entityStore.findAll(
                    method.getEntityInformation().getJavaType(),
                    entity -> {
                        context.setVariable("entity", entity);
                        return expression.getValue(context, Boolean.class);
                    }
            );
        } else {
            return entityStore.findOne(
                    method.getEntityInformation().getJavaType(),
                    entity -> {
                        context.setVariable("entity", entity);
                        return expression.getValue(context, Boolean.class);
                    }
            );
        }
    }

    @Override
    public QueryMethod getQueryMethod() {
        return method;
    }
}
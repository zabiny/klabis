package org.springframework.hateoas;

import club.klabis.members.application.KlabisSecurityServiceImpl;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(KlabisSecurityServiceImpl.class)
public class HateoasImprovementsAutoConfiguration implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SecurityUtils.setApplicationContext(applicationContext);
    }
}

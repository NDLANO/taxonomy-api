package no.ndla.taxonomy.service;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.ServletException;
import java.io.IOException;


@Configuration
public class LoggingConfiguration extends WebMvcConfigurerAdapter implements EmbeddedServletContainerCustomizer {

    Logger logger = LoggerFactory.getLogger("accesslog");

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        if (container instanceof TomcatEmbeddedServletContainerFactory) {
            TomcatEmbeddedServletContainerFactory factory = (TomcatEmbeddedServletContainerFactory) container;

            Valve accessLogValve = new ValveBase() {
                @Override
                public void invoke(Request request, Response response) throws IOException, ServletException {
                    MDC.put("remote_addr", request.getRemoteAddr());
                    logger.info(request.getMethod() + " " + request.getContextPath());
                }
            };

            factory.addContextValves(accessLogValve);
        } else {
            logger.error("WARNING! this customizer does not support your configured container");
        }
    }

}

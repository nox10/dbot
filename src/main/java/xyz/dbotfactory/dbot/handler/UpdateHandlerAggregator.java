package xyz.dbotfactory.dbot.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.DBotUserException;
import xyz.dbotfactory.dbot.model.Chat;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class UpdateHandlerAggregator implements BeanPostProcessor {

    private List<UpdateHandler> updateHandlers = new ArrayList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof UpdateHandler){
            updateHandlers.add((UpdateHandler) bean);
        }
        return bean;
    }

    public UpdateHandler getCommandHandler(Update update, Chat chat){
        List<UpdateHandler> collect = updateHandlers
                .stream()
                .filter(x -> x.canHandle(update, chat))
                .collect(toList());

        if(collect.size() == 0)
            throw new DBotUserException("No handler for update found");

        if(collect.size() > 1 )
            throw new DBotUserException("More than 1 handler for update found");

        return collect.get(0);
    }
}

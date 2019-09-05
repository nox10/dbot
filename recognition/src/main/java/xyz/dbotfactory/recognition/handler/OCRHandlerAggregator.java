package xyz.dbotfactory.recognition.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Component
public class OCRHandlerAggregator implements BeanPostProcessor {
    private List<OCRHandler> OCRHandlers = new ArrayList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof OCRHandler){
            OCRHandlers.add((OCRHandler) bean);
        }
        return bean;
    }

    public OCRResult tryOCRHandlers(String image){
        OCRHandlers.sort(Comparator.comparing(OCRHandler::getPriority));
        for (OCRHandler ocrHadnler : OCRHandlers) {
            OCRResult ocrHandleInfo = ocrHadnler.parseImage(image);
            if (ocrHandleInfo.isHandled())
                return ocrHandleInfo;
        }
        return OCRResult.builder().handled(false).build();
    }
}

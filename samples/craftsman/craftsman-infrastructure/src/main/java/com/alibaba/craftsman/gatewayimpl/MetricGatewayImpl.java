package com.alibaba.craftsman.gatewayimpl;

import com.alibaba.craftsman.common.event.DomainEventPublisher;
import com.alibaba.craftsman.domain.gateway.MetricGateway;
import com.alibaba.craftsman.domain.metrics.MainMetricType;
import com.alibaba.craftsman.domain.metrics.MetricItem;
import com.alibaba.craftsman.domain.metrics.SubMetric;
import com.alibaba.craftsman.domain.metrics.SubMetricType;
import com.alibaba.craftsman.domain.metrics.appquality.AppMetric;
import com.alibaba.craftsman.domain.metrics.appquality.AppMetricItem;
import com.alibaba.craftsman.domain.metrics.devquality.BugMetric;
import com.alibaba.craftsman.domain.metrics.devquality.BugMetricItem;
import com.alibaba.craftsman.domain.metrics.techcontribution.*;
import com.alibaba.craftsman.domain.metrics.techinfluence.*;
import com.alibaba.craftsman.dto.domainevent.MetricItemCreatedEvent;
import com.alibaba.craftsman.gatewayimpl.database.MetricMapper;
import com.alibaba.craftsman.gatewayimpl.database.dataobject.MetricDO;
import com.alibaba.craftsman.gatewayimpl.rpc.AppMetricMapper;
import com.alibaba.craftsman.gatewayimpl.rpc.BugMetricMapper;
import com.alibaba.craftsman.gatewayimpl.rpc.dataobject.AppMetricDO;
import com.alibaba.craftsman.gatewayimpl.rpc.dataobject.BugMetricDO;
import com.alibaba.craftsman.convertor.MetricConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MetricGatewayImpl
 *
 * @author Frank Zhang
 * @date 2020-07-02 12:20 PM
 */
@Component
@Slf4j
public class MetricGatewayImpl implements MetricGateway {

    @Resource
    private MetricMapper metricMapper;

    @Resource
    private BugMetricMapper bugMetricMapper;

    @Resource
    private AppMetricMapper appMetricMapper;

    @Resource
    private DomainEventPublisher domainEventPublisher;

    @Resource
    Validator validator;

    public void save(MetricItem metricItem){
        MetricDO metricDO = MetricConvertor.toDataObject(metricItem);

        // Set<ConstraintViolation<MetricDO>> violations =  validator.validate(metricDO);
        // if (!violations.isEmpty()) {
        //     throw new ConstraintViolationException(violations);
        // }
        metricMapper.create(metricDO);

        log.debug("AutoGeneratedId: "+metricDO.getId());
        MetricItemCreatedEvent metricItemCreatedEvent = new MetricItemCreatedEvent();
        metricItemCreatedEvent.setId(metricDO.getId());
        metricItemCreatedEvent.setUserId(metricDO.getUserId());
        metricItemCreatedEvent.setMainMetricType(metricDO.getMainMetric());
        domainEventPublisher.publish(metricItemCreatedEvent);
    }

    public List<SubMetric> listByTechContribution(String userId){
        List<MetricDO> metricDOList = metricMapper.listByMainMetric(userId, MainMetricType.TECH_CONTRIBUTION.getMetricCode());
        RefactoringMetric refactoringMetric = new RefactoringMetric();
        MiscMetric miscMetric = new MiscMetric();
        CodeReviewMetric codeReviewMetric = new CodeReviewMetric();
        List<SubMetric> subMetricList = new ArrayList<>();
        subMetricList.add(refactoringMetric);
        subMetricList.add(miscMetric);
        subMetricList.add(codeReviewMetric);
        metricDOList.forEach(metricDO -> {
            String json = metricDO.getMetricItem();
            switch (SubMetricType.valueOf(metricDO.getSubMetric())){
                case Refactoring:
                    refactoringMetric.addMetricItem(RefactoringMetricItem.valueOf(json));
                    break;
                case Misc:
                    miscMetric.addMetricItem(MiscMetricItem.valueOf(json));
                    break;
                case CodeReview:
                    codeReviewMetric.addMetricItem(CodeReviewMetricItem.valueOf(json));
                    break;
                default:
                    log.error("illegal SubMetric type: " + metricDO.getSubMetric());
            }
        });
        return subMetricList;
    }

    public List<SubMetric> listByTechInfluence(String userId){
        List<MetricDO> metricDOList = metricMapper.listByMainMetric(userId, MainMetricType.TECH_INFLUENCE.getMetricCode());
        ATAMetric ataMetric = new ATAMetric();
        SharingMetric sharingMetric = new SharingMetric();
        PatentMetric patentMetric = new PatentMetric();
        PaperMetric paperMetric = new PaperMetric();
        List<SubMetric> subMetricList = new ArrayList<>();
        subMetricList.add(ataMetric);
        subMetricList.add(sharingMetric);
        subMetricList.add(patentMetric);
        subMetricList.add(paperMetric);
        metricDOList.forEach(metricDO -> {
            String json = metricDO.getMetricItem();
            switch (SubMetricType.valueOf(metricDO.getSubMetric())){
                case ATA:
                    ataMetric.addMetricItem(ATAMetricItem.valueOf(json));
                    break;
                case Sharing:
                    sharingMetric.addMetricItem(SharingMetricItem.valueOf(json));
                    break;
                case Patent:
                    patentMetric.addMetricItem(PatentMetricItem.valueOf(json));
                    break;
                case Paper:
                    paperMetric.addMetricItem(PaperMetricItem.valueOf(json));
                default:
                    log.error("illegal SubMetric type: " + metricDO.getSubMetric());
            }
        });
        return subMetricList;
    }

    public BugMetric getBugMetric(String userId){
        BugMetricDO bugMetricDO = bugMetricMapper.getByUserId(userId);
        BugMetricItem bugMetricItem = new BugMetricItem(bugMetricDO.getBugCount(), bugMetricDO.getCheckInCodeCount());
        BugMetric bugMetric = new BugMetric();
        bugMetric.addMetricItem(bugMetricItem);
        return bugMetric;
    }

    public AppMetric getAppMetric(String userId){
        List<AppMetricDO> appMetricDOList = appMetricMapper.listByUserId(userId);
        AppMetric appMetric = new AppMetric();
        appMetricDOList.forEach(appMetricDO -> {
            AppMetricItem appMetricItem = new AppMetricItem();
            BeanUtils.copyProperties(appMetricDO, appMetricItem);
            appMetric.addMetricItem(appMetricItem);
        });
        return appMetric;
    }
}

package com.scmt.activiti.serviceimpl;

import com.scmt.activiti.dao.ActProcessDao;
import com.scmt.activiti.entity.ActBusiness;
import com.scmt.activiti.entity.ActProcess;
import com.scmt.activiti.service.ActNodeService;
import com.scmt.activiti.service.ActProcessService;
import com.scmt.activiti.service.ActStarterService;
import com.scmt.activiti.utils.MessageUtil;
import com.scmt.activiti.vo.ProcessNodeVo;
import com.scmt.core.common.constant.ActivitiConstant;
import com.scmt.core.common.exception.ScmtException;
import com.scmt.core.common.utils.SecurityUtil;
import com.scmt.core.common.vo.SearchVo;
import com.scmt.core.entity.DepartmentHeader;
import com.scmt.core.entity.Role;
import com.scmt.core.entity.User;
import com.scmt.core.service.DepartmentHeaderService;
import com.scmt.core.service.UserRoleService;
import com.scmt.core.service.UserService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.*;

/**
 * ????????????????????????
 * @author Exrick
 */
@Slf4j
@Service
@Transactional
public class ActProcessServiceImpl implements ActProcessService {

    @Autowired
    private ActProcessDao actProcessDao;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ActNodeService actNodeService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentHeaderService departmentHeaderService;

    @Autowired
    private ActProcessService actProcessService;

    @Autowired
    private ActStarterService actStarterService;

    @Autowired
    private MessageUtil messageUtil;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ActProcessDao getRepository() {
        return actProcessDao;
    }

    @Override
    public Page<ActProcess> findByCondition(Boolean showLatest, ActProcess actProcess, SearchVo searchVo, Pageable pageable) {

        return actProcessDao.findAll(new Specification<ActProcess>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<ActProcess> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                Path<String> nameField = root.get("name");
                Path<String> processKeyField = root.get("processKey");
                Path<String> categoryIdField = root.get("categoryId");
                Path<Integer> statusField = root.get("status");
                Path<Date> createTimeField = root.get("createTime");
                Path<Boolean> latestField = root.get("latest");

                List<Predicate> list = new ArrayList<>();

                // ????????????
                if (StrUtil.isNotBlank(actProcess.getName())) {
                    list.add(cb.like(nameField, '%' + actProcess.getName() + '%'));
                }
                if (StrUtil.isNotBlank(actProcess.getProcessKey())) {
                    list.add(cb.like(processKeyField, '%' + actProcess.getProcessKey() + '%'));
                }

                // ??????
                if (StrUtil.isNotBlank(actProcess.getCategoryId())) {
                    list.add(cb.equal(categoryIdField, actProcess.getCategoryId()));
                }

                // ??????
                if (actProcess.getStatus() != null) {
                    list.add(cb.equal(statusField, actProcess.getStatus()));
                }
                // ????????????
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }

                // ???????????????
                if (showLatest != null && showLatest) {
                    list.add(cb.equal(latestField, true));
                }

                Predicate[] arr = new Predicate[list.size()];
                cq.where(list.toArray(arr));
                return null;
            }
        }, pageable);
    }

    @Override
    public ActProcess findByProcessKeyAndLatest(String processKey, Boolean latest) {

        return actProcessDao.findByProcessKeyAndLatest(processKey, latest);
    }

    @Override
    public void setAllOldByProcessKey(String processKey) {

        List<ActProcess> list = actProcessDao.findByProcessKey(processKey);
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(item -> {
            item.setLatest(false);
        });
        actProcessDao.saveAll(list);
    }

    @Override
    public void setLatestByProcessKey(String processKey) {

        ActProcess actProcess = actProcessDao.findTopByProcessKeyOrderByVersionDesc(processKey);
        if (actProcess == null) {
            return;
        }
        actProcess.setLatest(true);
        actProcessDao.save(actProcess);
    }

    @Override
    public List<ActProcess> findByCategoryId(String categoryId) {

        return actProcessDao.findByCategoryId(categoryId);
    }

    @Override
    public String startProcess(ActBusiness actBusiness) {

        String userId = securityUtil.getCurrUser().getId();
        // ????????????
        ActProcess actProcess = actProcessService.get(actBusiness.getProcDefId());
        if (actProcess == null) {
            throw new ScmtException("?????????????????????");
        }
        if (actProcess.getAllUser() != null && !actProcess.getAllUser()) {
            // ??????????????????
            if (!actStarterService.hasRecord(actBusiness.getProcDefId(), userId)) {
                throw new ScmtException("?????????????????????????????????");
            }
        }
        // ??????????????????
        identityService.setAuthenticatedUserId(userId);
        // ???????????? ??????????????????id??????
        actBusiness.getParams().put("tableId", actBusiness.getTableId());
        ProcessInstance pi = runtimeService.startProcessInstanceById(actBusiness.getProcDefId(), actBusiness.getId(), actBusiness.getParams());
        // ????????????????????????
        runtimeService.setProcessInstanceName(pi.getId(), actBusiness.getTitle());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        for (Task task : tasks) {
            if (actBusiness.getFirstGateway()) {
                // ????????????
                List<User> users = getNode(task.getTaskDefinitionKey()).getUsers();
                // ?????????????????????????????????????????? ??????????????????
                if (users == null || users.isEmpty()) {
                    throw new ScmtException("??????????????????????????????????????????????????????????????????");
                } else {
                    // ???????????????????????????????????????
                    for (User user : users) {
                        taskService.addCandidateUser(task.getId(), user.getId());
                        // ???????????????
                        messageUtil.sendActMessage(user.getId(), ActivitiConstant.MESSAGE_TODO_CONTENT, actBusiness.getSendMessage(),
                                actBusiness.getSendSms(), actBusiness.getSendEmail());
                    }
                }
            } else {
                // ???????????????????????????
                for (String assignee : actBusiness.getAssignees()) {
                    taskService.addCandidateUser(task.getId(), assignee);
                    // ???????????????
                    messageUtil.sendActMessage(assignee, ActivitiConstant.MESSAGE_TODO_CONTENT, actBusiness.getSendMessage(),
                            actBusiness.getSendSms(), actBusiness.getSendEmail());
                }
            }
            // ?????????????????????
            taskService.setPriority(task.getId(), actBusiness.getPriority());
        }
        return pi.getId();
    }

    @Override
    public ProcessNodeVo getFirstNode(String procDefId) {

        BpmnModel bpmnModel = repositoryService.getBpmnModel(procDefId);

        ProcessNodeVo node = new ProcessNodeVo();

        List<Process> processes = bpmnModel.getProcesses();
        Collection<FlowElement> elements = processes.get(0).getFlowElements();
        // ??????????????????
        StartEvent startEvent = null;
        for (FlowElement element : elements) {
            if (element instanceof StartEvent) {
                startEvent = (StartEvent) element;
                break;
            }
        }
        FlowElement e = null;
        if (startEvent == null) {
            throw new ScmtException("??????????????????????????????????????????????????????????????????");
        }
        // ??????????????????????????????
        SequenceFlow sequenceFlow = startEvent.getOutgoingFlows().get(0);
        for (FlowElement element : elements) {
            if (element.getId().equals(sequenceFlow.getTargetRef())) {
                if (element instanceof UserTask) {
                    e = element;
                    node.setType(ActivitiConstant.NODE_TYPE_TASK);
                    break;
                } else if (element instanceof ExclusiveGateway) {
                    e = element;
                    node.setType(ActivitiConstant.NODE_TYPE_EG);
                    break;
                } else if (element instanceof ParallelGateway) {
                    e = element;
                    node.setType(ActivitiConstant.NODE_TYPE_PG);
                    break;
                } else {
                    throw new ScmtException("?????????????????????????????????????????????????????????????????????????????????????????????");
                }
            }
        }
        if (e == null) {
            throw new ScmtException("??????????????????????????????????????????????????????????????????????????????");
        }
        // ?????????????????????????????????
        if (e instanceof ExclusiveGateway || e instanceof ParallelGateway) {
            return node;
        }
        node.setTitle(e.getName());
        // ???????????????????????????
        Boolean customUser = actNodeService.hasCustomUser(e.getId());
        if (customUser) {
            node.setType(ActivitiConstant.NODE_TYPE_CUSTOM);
        } else {
            // ??????????????????
            List<User> users = getNodetUsers(e.getId());
            node.setUsers(removeDuplicate(users));
        }
        return node;
    }

    @Override
    public ProcessNodeVo getNextNode(String procDefId, String currActId) {

        ProcessNodeVo node = new ProcessNodeVo();

        // ??????????????????id
        ProcessDefinitionEntity dfe = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(procDefId);
        // ??????????????????
        List<ActivityImpl> activitiList = dfe.getActivities();
        // ???????????????????????????????????????????????????????????????????????????
        for (ActivityImpl activityImpl : activitiList) {
            if (activityImpl.getId().equals(currActId)) {
                // ?????????????????????
                List<PvmTransition> pvmTransitions = activityImpl.getOutgoingTransitions();

                PvmActivity pvmActivity = pvmTransitions.get(0).getDestination();

                String type = pvmActivity.getProperty("type").toString();
                if ("userTask".equals(type)) {
                    // ??????????????????
                    Boolean customUser = actNodeService.hasCustomUser(pvmActivity.getId());
                    if (customUser) {
                        // ???????????????????????????
                        node.setType(ActivitiConstant.NODE_TYPE_CUSTOM);
                    } else {
                        node.setType(ActivitiConstant.NODE_TYPE_TASK);
                        node.setTitle(pvmActivity.getProperty("name").toString());
                        // ??????????????????
                        List<User> users = getNodetUsers(pvmActivity.getId());
                        node.setUsers(removeDuplicate(users));
                    }
                } else if ("exclusiveGateway".equals(type)) {
                    // ????????????
                    node.setType(ActivitiConstant.NODE_TYPE_EG);
                } else if ("parallelGateway".equals(type)) {
                    // ????????????
                    node.setType(ActivitiConstant.NODE_TYPE_PG);
                } else if ("endEvent".equals(type)) {
                    // ??????
                    node.setType(ActivitiConstant.NODE_TYPE_END);
                } else {
                    throw new ScmtException("????????????????????????????????????????????????");
                }
                break;
            }
        }

        return node;
    }

    @Override
    public ProcessNodeVo getNode(String nodeId) {

        return getNode(nodeId, null);
    }

    @Override
    public ProcessNodeVo getNode(String nodeId, String taskId) {

        ProcessNodeVo node = new ProcessNodeVo();
        // ?????????????????????????????????????????????????????????
        if (actNodeService.hasCustomUser(nodeId)) {
            if (StrUtil.isBlank(taskId)) {
                throw new ScmtException("??????????????????taskId????????????");
            }
            node.setCustomUser(true);
            // ?????????????????????
            List<HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForTask(taskId);
            List<User> users = new ArrayList<>();
            links.forEach(link -> {
                if (IdentityLinkType.CANDIDATE.equals(link.getType())) {
                    users.add(userService.get(link.getUserId()));
                }
            });
            node.setUsers(removeDuplicate(users));
        } else {
            // ??????????????????
            List<User> users = getNodetUsers(nodeId);
            node.setUsers(removeDuplicate(users));
        }
        return node;
    }

    @Override
    public void updateCategoryTitle(String categoryId, String categoryTitle) {

        actProcessDao.updateCategoryTitle(categoryId, categoryTitle);
    }

    /**
     * ?????????????????????
     * @param nodeId
     */
    public List<User> getNodetUsers(String nodeId) {

        List<User> users = actNodeService.findUserByNodeId(nodeId);
        // ???????????????????????????
        List<Role> roles = actNodeService.findRoleByNodeId(nodeId);
        for (Role r : roles) {
            List<User> userList = userRoleService.findUserByRoleId(r.getId());
            users.addAll(userList);
        }
        // ???????????????????????????
        List<String> departmentIds = actNodeService.findDepartmentIdsByNodeId(nodeId);
        List<DepartmentHeader> departments = departmentHeaderService.findByDepartmentIdIn(departmentIds);
        for (DepartmentHeader d : departments) {
            User user = userService.get(d.getUserId());
            users.add(user);
        }
        // ???????????????????????????
        if (actNodeService.hasChooseDepHeader(nodeId)) {
            String depId = securityUtil.getCurrUser().getDepartmentId();
            if (StrUtil.isNotBlank(depId)) {
                List<DepartmentHeader> departmentHeaders = departmentHeaderService.findByDepartmentIdIn(Collections.singletonList(depId));
                for (DepartmentHeader d : departmentHeaders) {
                    User user = userService.get(d.getUserId());
                    users.add(user);
                }
            }
        }
        return users;
    }

    /**
     * ??????
     * @param list
     * @return
     */
    private List<User> removeDuplicate(List<User> list) {

        LinkedHashSet<User> set = new LinkedHashSet<>(list.size());
        set.addAll(list);
        list.clear();
        list.addAll(set);
        entityManager.clear();
        list.forEach(u -> {
            u.setPassword(null);
        });
        return list;
    }
}

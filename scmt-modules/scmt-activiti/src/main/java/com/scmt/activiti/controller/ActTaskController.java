package com.scmt.activiti.controller;

import com.scmt.activiti.entity.ActBusiness;
import com.scmt.activiti.entity.ActProcess;
import com.scmt.activiti.service.ActBusinessService;
import com.scmt.activiti.service.ActProcessService;
import com.scmt.activiti.service.mybatis.IHistoryIdentityService;
import com.scmt.activiti.service.mybatis.IRunIdentityService;
import com.scmt.activiti.utils.MessageUtil;
import com.scmt.activiti.vo.*;
import com.scmt.core.common.constant.ActivitiConstant;
import com.scmt.core.common.exception.ScmtException;
import com.scmt.core.common.utils.ResultUtil;
import com.scmt.core.common.utils.SecurityUtil;
import com.scmt.core.common.utils.SnowFlakeUtil;
import com.scmt.core.common.vo.PageVo;
import com.scmt.core.common.vo.Result;
import com.scmt.core.common.vo.SearchVo;
import com.scmt.core.entity.User;
import com.scmt.core.service.UserService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Exrick
 */
@Slf4j
@RestController
@Api(description = "??????????????????")
@RequestMapping("/scmt/actTask")
@Transactional
public class ActTaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private ActProcessService actProcessService;

    @Autowired
    private ActBusinessService actBusinessService;

    @Autowired
    private UserService userService;

    @Autowired
    private IHistoryIdentityService iHistoryIdentityService;

    @Autowired
    private IRunIdentityService iRunIdentityService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private MessageUtil messageUtil;

    @RequestMapping(value = "/todoList", method = RequestMethod.GET)
    @ApiOperation(value = "????????????")
    public Result<Object> todoList(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) String categoryId,
                                   @RequestParam(required = false) Integer priority,
                                   SearchVo searchVo,
                                   PageVo pageVo) {

        ActPage<TaskVo> page = new ActPage<TaskVo>();
        List<TaskVo> list = new ArrayList<>();

        String userId = securityUtil.getCurrUser().getId();
        TaskQuery query = taskService.createTaskQuery().taskCandidateOrAssigned(userId);

        // ???????????????
        if ("createTime".equals(pageVo.getSort()) && "asc".equals(pageVo.getOrder())) {
            query.orderByTaskCreateTime().asc();
        } else if ("priority".equals(pageVo.getSort()) && "asc".equals(pageVo.getOrder())) {
            query.orderByTaskPriority().asc();
        } else if ("priority".equals(pageVo.getSort()) && "desc".equals(pageVo.getOrder())) {
            query.orderByTaskPriority().desc();
        } else {
            query.orderByTaskCreateTime().desc();
        }
        if (StrUtil.isNotBlank(name)) {
            query.taskNameLike("%" + name + "%");
        }
        if (StrUtil.isNotBlank(categoryId)) {
            query.taskCategory(categoryId);
        }
        if (priority != null) {
            query.taskPriority(priority);
        }
        if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
            Date start = DateUtil.parse(searchVo.getStartDate());
            Date end = DateUtil.parse(searchVo.getEndDate());
            query.taskCreatedAfter(start);
            query.taskCreatedBefore(DateUtil.endOfDay(end));
        }

        page.setTotalElements(query.count());
        int first = (pageVo.getPageNumber() - 1) * pageVo.getPageSize();
        List<Task> taskList = query.listPage(first, pageVo.getPageSize());

        // ??????vo
        taskList.forEach(e -> {
            TaskVo tv = new TaskVo(e);

            // ???????????????
            if (StrUtil.isNotBlank(tv.getOwner())) {
                User o = userService.get(tv.getOwner());
                tv.setOwner(o.getNickname()).setOwnerUsername(o.getUsername());
            }
            List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(tv.getProcInstId());
            for (IdentityLink ik : identityLinks) {
                // ???????????????
                if (IdentityLinkType.STARTER.equals(ik.getType()) && StrUtil.isNotBlank(ik.getUserId())) {
                    User s = userService.get(ik.getUserId());
                    tv.setApplyer(s.getNickname()).setApplyerUsername(s.getUsername());
                }
            }
            // ??????????????????
            ActProcess actProcess = actProcessService.get(tv.getProcDefId());
            if (actProcess != null) {
                tv.setProcessName(actProcess.getName());
                tv.setRouteName(actProcess.getRouteName());
                tv.setVersion(actProcess.getVersion());
            }
            // ????????????key
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(tv.getProcInstId()).singleResult();
            tv.setBusinessKey(pi.getBusinessKey());
            ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
            if (actBusiness != null) {
                tv.setTableId(actBusiness.getTableId());
            }

            list.add(tv);
        });
        page.setContent(list);
        return ResultUtil.data(page);
    }

    @RequestMapping(value = "/doneList", method = RequestMethod.GET)
    @ApiOperation(value = "????????????")
    public Result<Object> doneList(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) String categoryId,
                                   @RequestParam(required = false) Integer priority,
                                   SearchVo searchVo,
                                   PageVo pageVo) {

        ActPage<HistoricTaskVo> page = new ActPage<HistoricTaskVo>();
        List<HistoricTaskVo> list = new ArrayList<>();

        String userId = securityUtil.getCurrUser().getId();
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().or().taskCandidateUser(userId).
                taskAssignee(userId).endOr().finished();

        // ???????????????
        if ("createTime".equals(pageVo.getSort()) && "asc".equals(pageVo.getOrder())) {
            query.orderByHistoricTaskInstanceEndTime().asc();
        } else if ("priority".equals(pageVo.getSort()) && "asc".equals(pageVo.getOrder())) {
            query.orderByTaskPriority().asc();
        } else if ("priority".equals(pageVo.getSort()) && "desc".equals(pageVo.getOrder())) {
            query.orderByTaskPriority().desc();
        } else if ("duration".equals(pageVo.getSort()) && "asc".equals(pageVo.getOrder())) {
            query.orderByHistoricTaskInstanceDuration().asc();
        } else if ("duration".equals(pageVo.getSort()) && "desc".equals(pageVo.getOrder())) {
            query.orderByHistoricTaskInstanceDuration().desc();
        } else {
            query.orderByHistoricTaskInstanceEndTime().desc();
        }
        if (StrUtil.isNotBlank(name)) {
            query.taskNameLike("%" + name + "%");
        }
        if (StrUtil.isNotBlank(categoryId)) {
            query.taskCategory(categoryId);
        }
        if (priority != null) {
            query.taskPriority(priority);
        }
        if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
            Date start = DateUtil.parse(searchVo.getStartDate());
            Date end = DateUtil.parse(searchVo.getEndDate());
            query.taskCompletedAfter(start);
            query.taskCompletedBefore(DateUtil.endOfDay(end));
        }

        page.setTotalElements((long) query.list().size());
        int first = (pageVo.getPageNumber() - 1) * pageVo.getPageSize();
        List<HistoricTaskInstance> taskList = query.listPage(first, pageVo.getPageSize());
        // ??????vo
        taskList.forEach(e -> {
            HistoricTaskVo htv = new HistoricTaskVo(e);
            // ???????????????
            if (StrUtil.isNotBlank(htv.getOwner())) {
                User o = userService.get(htv.getOwner());
                htv.setOwner(o.getNickname()).setOwnerUsername(o.getUsername());
            }
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForProcessInstance(htv.getProcInstId());
            for (HistoricIdentityLink hik : identityLinks) {
                // ???????????????
                if (IdentityLinkType.STARTER.equals(hik.getType()) && StrUtil.isNotBlank(hik.getUserId())) {
                    User s = userService.get(hik.getUserId());
                    htv.setApplyer(s.getNickname()).setApplyerUsername(s.getUsername());
                }
            }
            // ??????????????????
            List<Comment> comments = taskService.getTaskComments(htv.getId(), "comment");
            if (comments != null && comments.size() > 0) {
                htv.setComment(comments.get(0).getFullMessage());
            }
            // ??????????????????
            ActProcess actProcess = actProcessService.get(htv.getProcDefId());
            if (actProcess != null) {
                htv.setProcessName(actProcess.getName());
                htv.setRouteName(actProcess.getRouteName());
                htv.setVersion(actProcess.getVersion());
            }
            // ????????????key
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(htv.getProcInstId()).singleResult();
            htv.setBusinessKey(hpi.getBusinessKey());
            ActBusiness actBusiness = actBusinessService.get(hpi.getBusinessKey());
            if (actBusiness != null) {
                htv.setTableId(actBusiness.getTableId());
            }

            list.add(htv);
        });
        page.setContent(list);
        return ResultUtil.data(page);
    }

    @RequestMapping(value = "/historicFlow/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????")
    public Result<Object> historicFlow(@ApiParam("????????????id") @PathVariable String id) {

        List<HistoricTaskVo> list = new ArrayList<>();

        List<HistoricTaskInstance> taskList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(id).orderByHistoricTaskInstanceEndTime().asc().list();

        // ??????vo
        taskList.forEach(e -> {
            HistoricTaskVo htv = new HistoricTaskVo(e);
            List<Assignee> assignees = new ArrayList<>();
            // ????????????????????????????????????????????????
            if (StrUtil.isNotBlank(htv.getAssignee())) {
                User assignee = userService.get(htv.getAssignee());
                User owner = userService.get(htv.getOwner());
                assignees.add(new Assignee(assignee.getNickname(),
                        assignee.getUsername() + "?????? " + owner.getNickname() + "(" + owner.getUsername() + ") ?????????", true));
            }
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(e.getId());
            // ????????????????????????id
            String userId = iHistoryIdentityService.findUserIdByTypeAndTaskId(ActivitiConstant.EXECUTOR_TYPE, e.getId());

            for (HistoricIdentityLink hik : identityLinks) {
                // ??????????????????????????????????????????????????????
                if (IdentityLinkType.CANDIDATE.equals(hik.getType()) && StrUtil.isNotBlank(hik.getUserId())) {
                    User u = userService.get(hik.getUserId());
                    Assignee assignee = new Assignee(u.getNickname(), u.getUsername(), false);
                    if (StrUtil.isNotBlank(userId) && userId.equals(hik.getUserId())) {
                        assignee.setIsExecutor(true);
                    }
                    assignees.add(assignee);
                }
            }
            htv.setAssignees(assignees);
            // ??????????????????
            List<Comment> comments = taskService.getTaskComments(htv.getId(), "comment");
            if (comments != null && comments.size() > 0) {
                htv.setComment(comments.get(0).getFullMessage());
            }
            list.add(htv);
        });
        return ResultUtil.data(list);
    }

    @RequestMapping(value = "/pass", method = RequestMethod.POST)
    @ApiOperation(value = "????????????????????????")
    public Result<Object> pass(@ApiParam("??????id") @RequestParam String id,
                               @ApiParam("????????????id") @RequestParam String procInstId,
                               @ApiParam("?????????????????????") @RequestParam(required = false) String[] assignees,
                               @ApiParam("?????????") @RequestParam(required = false) Integer priority,
                               @ApiParam("????????????") @RequestParam(required = false) String comment,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail) {

        taskService.addComment(id, procInstId, StrUtil.isBlank(comment) ? "" : comment);
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        Task task = taskService.createTaskQuery().taskId(id).singleResult();
        if (StrUtil.isNotBlank(task.getOwner()) && !DelegationState.RESOLVED.equals(task.getDelegationState())) {
            // ???????????????????????? ???resolve
            String oldAssignee = task.getAssignee();
            taskService.resolveTask(id);
            taskService.setAssignee(id, oldAssignee);
        }
        taskService.complete(id);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInstId).list();
        // ?????????????????????
        if (tasks != null && tasks.size() > 0) {
            for (Task t : tasks) {
                if (assignees == null || assignees.length < 1) {
                    // ?????????????????????????????????????????? ??????????????????
                    List<User> users = actProcessService.getNode(t.getTaskDefinitionKey()).getUsers();
                    if (users == null || users.isEmpty()) {
                        runtimeService.deleteProcessInstance(procInstId, "canceled-?????????????????????????????????????????????????????????");
                        ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
                        actBusiness.setStatus(ActivitiConstant.STATUS_CANCELED);
                        actBusiness.setResult(ActivitiConstant.RESULT_TO_SUBMIT);
                        actBusinessService.update(actBusiness);
                        break;
                    } else {
                        // ??????????????????
                        List<String> list = iRunIdentityService.selectByConditions(t.getId(), IdentityLinkType.CANDIDATE);
                        if (list == null || list.isEmpty()) {
                            // ???????????????????????????????????????
                            for (User user : users) {
                                taskService.addCandidateUser(t.getId(), user.getId());
                                // ???????????????
                                messageUtil.sendActMessage(user.getId(), ActivitiConstant.MESSAGE_TODO_CONTENT, sendMessage, sendSms, sendEmail);
                            }
                            taskService.setPriority(t.getId(), task.getPriority());
                        }
                    }
                } else {
                    // ??????????????????
                    List<String> list = iRunIdentityService.selectByConditions(t.getId(), IdentityLinkType.CANDIDATE);
                    if (list == null || list.isEmpty()) {
                        for (String assignee : assignees) {
                            taskService.addCandidateUser(t.getId(), assignee);
                            // ???????????????
                            messageUtil.sendActMessage(assignee, ActivitiConstant.MESSAGE_TODO_CONTENT, sendMessage, sendSms, sendEmail);
                            taskService.setPriority(t.getId(), priority);
                        }
                    }
                }
            }
        } else {
            ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
            actBusiness.setStatus(ActivitiConstant.STATUS_FINISH);
            actBusiness.setResult(ActivitiConstant.RESULT_PASS);
            actBusinessService.update(actBusiness);
            // ???????????????
            messageUtil.sendActMessage(actBusiness.getUserId(), ActivitiConstant.MESSAGE_PASS_CONTENT, sendMessage, sendSms, sendEmail);
        }
        // ????????????????????????
        iHistoryIdentityService.insert(SnowFlakeUtil.nextId().toString(),
                ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, procInstId);
        return ResultUtil.success("????????????");
    }

    @RequestMapping(value = "/passAll", method = RequestMethod.POST)
    @ApiOperation(value = "????????????")
    public Result<Object> passAll(@ApiParam("??????ids") @RequestParam String[] ids,
                                  @ApiParam("????????????") @RequestParam(required = false) String comment,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail) {

        int count = 0;
        for (String id : ids) {
            Task task = taskService.createTaskQuery().taskId(id).singleResult();
            taskService.addComment(id, task.getProcessInstanceId(), StrUtil.isBlank(comment) ? "" : comment);
            ProcessNodeVo next = actProcessService.getNextNode(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
            if (ActivitiConstant.NODE_TYPE_CUSTOM.equals(next.getType())) {
                count++;
                continue;
            }
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            if (StrUtil.isNotBlank(task.getOwner()) && !DelegationState.RESOLVED.equals(task.getDelegationState())) {
                String oldAssignee = task.getAssignee();
                taskService.resolveTask(id);
                taskService.setAssignee(id, oldAssignee);
            }
            taskService.complete(id);
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
            // ?????????????????????
            if (tasks != null && tasks.size() > 0) {
                for (Task t : tasks) {
                    List<User> users = actProcessService.getNode(t.getTaskDefinitionKey()).getUsers();
                    // ?????????????????????????????????????????? ??????????????????
                    if (users == null || users.isEmpty()) {
                        runtimeService.deleteProcessInstance(pi.getId(), "canceled-?????????????????????????????????????????????????????????");
                        ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
                        actBusiness.setStatus(ActivitiConstant.STATUS_CANCELED);
                        actBusiness.setResult(ActivitiConstant.RESULT_TO_SUBMIT);
                        actBusinessService.update(actBusiness);
                        break;
                    } else {
                        // ??????????????????
                        List<String> list = iRunIdentityService.selectByConditions(t.getId(), IdentityLinkType.CANDIDATE);
                        if (list == null || list.isEmpty()) {
                            // ???????????????????????????????????????
                            for (User user : users) {
                                taskService.addCandidateUser(t.getId(), user.getId());
                                // ???????????????
                                messageUtil.sendActMessage(user.getId(), ActivitiConstant.MESSAGE_TODO_CONTENT, sendMessage, sendSms, sendEmail);
                                taskService.setPriority(t.getId(), task.getPriority());
                            }
                        }
                    }
                }
            } else {
                ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
                actBusiness.setStatus(ActivitiConstant.STATUS_FINISH);
                actBusiness.setResult(ActivitiConstant.RESULT_PASS);
                actBusinessService.update(actBusiness);
                // ???????????????
                messageUtil.sendActMessage(actBusiness.getUserId(), ActivitiConstant.MESSAGE_PASS_CONTENT, sendMessage, sendSms, sendEmail);
            }
            // ????????????????????????
            iHistoryIdentityService.insert(SnowFlakeUtil.nextId().toString(),
                    ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, pi.getId());
        }
        String customCount = "";
        if (count > 0) {
            customCount = "????????????" + count + "????????????????????????";
        }
        return ResultUtil.success("?????????????????????" + (ids.length - count) + "?????????" + customCount);
    }

    @RequestMapping(value = "/getBackList/{procInstId}", method = RequestMethod.GET)
    @ApiOperation(value = "????????????????????????")
    public Result<Object> getBackList(@PathVariable String procInstId) {

        List<HistoricTaskVo> list = new ArrayList<>();
        List<HistoricTaskInstance> taskInstanceList = historyService.createHistoricTaskInstanceQuery().processInstanceId(procInstId)
                .finished().list();

        taskInstanceList.forEach(e -> {
            HistoricTaskVo htv = new HistoricTaskVo(e);
            list.add(htv);
        });

        // ????????????key??????
        LinkedHashSet<String> set = new LinkedHashSet<String>(list.size());
        List<HistoricTaskVo> newList = new ArrayList<>();
        list.forEach(e -> {
            if (set.add(e.getKey())) {
                newList.add(e);
            }
        });

        return ResultUtil.data(newList);
    }

    @RequestMapping(value = "/backToTask", method = RequestMethod.POST)
    @ApiOperation(value = "?????????????????????????????????????????????")
    public Result<Object> backToTask(@ApiParam("??????id") @RequestParam String id,
                                     @ApiParam("??????????????????key") @RequestParam String backTaskKey,
                                     @ApiParam("????????????id") @RequestParam String procInstId,
                                     @ApiParam("????????????id") @RequestParam String procDefId,
                                     @ApiParam("??????????????????") @RequestParam(required = false) String[] assignees,
                                     @ApiParam("?????????") @RequestParam(required = false) Integer priority,
                                     @ApiParam("????????????") @RequestParam(required = false) String comment,
                                     @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                                     @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                                     @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail) {


        judgeParallelGateway(procDefId);
        taskService.addComment(id, procInstId, StrUtil.isBlank(comment) ? "" : comment);
        // ??????????????????
        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(procDefId);
        // ?????????????????????Activity
        ActivityImpl hisActivity = definition.findActivity(backTaskKey);
        // ????????????
        managementService.executeCommand(new JumpTask(procInstId, hisActivity.getId()));
        // ??????????????????????????????
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInstId).list();
        if (tasks != null && tasks.size() > 0) {
            tasks.forEach(e -> {
                for (String assignee : assignees) {
                    taskService.addCandidateUser(e.getId(), assignee);
                    // ???????????????
                    messageUtil.sendActMessage(assignee, ActivitiConstant.MESSAGE_TODO_CONTENT, sendMessage, sendSms, sendEmail);
                }
                if (priority != null) {
                    taskService.setPriority(e.getId(), priority);
                }
            });
        }
        // ????????????????????????
        iHistoryIdentityService.insert(SnowFlakeUtil.nextId().toString(),
                ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, procInstId);
        return ResultUtil.success("????????????");
    }

    public void judgeParallelGateway(String procDefId) {

        ProcessDefinitionEntity dfe = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(procDefId);
        // ????????????????????????????????????????????????
        for (ActivityImpl activityImpl : dfe.getActivities()) {
            String type = activityImpl.getProperty("type").toString();
            if ("parallelGateway".equals(type)) {
                throw new ScmtException("???????????????????????????????????????????????????????????????????????????????????????");
            }
        }
    }

    @RequestMapping(value = "/back", method = RequestMethod.POST)
    @ApiOperation(value = "????????????????????????????????????")
    public Result<Object> back(@ApiParam("??????id") @RequestParam String id,
                               @ApiParam("????????????id") @RequestParam String procInstId,
                               @ApiParam("????????????") @RequestParam(required = false) String comment,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                               @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail) {


        taskService.addComment(id, procInstId, StrUtil.isBlank(comment) ? "" : comment);
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        // ??????????????????
        runtimeService.deleteProcessInstance(procInstId, ActivitiConstant.BACKED_FLAG);
        ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
        actBusiness.setStatus(ActivitiConstant.STATUS_FINISH);
        actBusiness.setResult(ActivitiConstant.RESULT_FAIL);
        actBusinessService.update(actBusiness);
        // ???????????????
        messageUtil.sendActMessage(actBusiness.getUserId(), ActivitiConstant.MESSAGE_BACK_CONTENT, sendMessage, sendSms, sendEmail);
        // ????????????????????????
        iHistoryIdentityService.insert(SnowFlakeUtil.nextId().toString(),
                ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, procInstId);
        return ResultUtil.success("????????????");
    }

    @RequestMapping(value = "/backAll", method = RequestMethod.POST)
    @ApiOperation(value = "????????????????????????")
    public Result<Object> backAll(@ApiParam("????????????ids") @RequestParam String[] procInstIds,
                                  @ApiParam("????????????") @RequestParam(required = false) String comment,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                                  @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail) {

        for (String procInstId : procInstIds) {
            // ????????????????????????
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInstId).list();
            tasks.forEach(t -> {
                taskService.addComment(t.getId(), procInstId, StrUtil.isBlank(comment) ? "" : comment);
                iHistoryIdentityService.insert(SnowFlakeUtil.nextId().toString(),
                        ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), t.getId(), procInstId);
            });
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
            // ??????????????????
            try {
                runtimeService.deleteProcessInstance(procInstId, ActivitiConstant.BACKED_FLAG);
            } catch (Exception e) {
                throw new ScmtException("????????????????????????????????????????????????????????????????????????");
            }
            ActBusiness actBusiness = actBusinessService.get(pi.getBusinessKey());
            actBusiness.setStatus(ActivitiConstant.STATUS_FINISH);
            actBusiness.setResult(ActivitiConstant.RESULT_FAIL);
            actBusinessService.update(actBusiness);
            // ???????????????
            messageUtil.sendActMessage(actBusiness.getUserId(), ActivitiConstant.MESSAGE_BACK_CONTENT, sendMessage, sendSms, sendEmail);
        }
        return ResultUtil.success("????????????");
    }

    @RequestMapping(value = "/delegate", method = RequestMethod.POST)
    @ApiOperation(value = "??????????????????")
    public Result<Object> delegate(@ApiParam("??????id") @RequestParam String id,
                                   @ApiParam("????????????id") @RequestParam String userId,
                                   @ApiParam("????????????id") @RequestParam String procInstId,
                                   @ApiParam("????????????") @RequestParam(required = false) String comment,
                                   @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendMessage,
                                   @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendSms,
                                   @ApiParam("????????????????????????") @RequestParam(defaultValue = "false") Boolean sendEmail) {

        taskService.addComment(id, procInstId, StrUtil.isBlank(comment) ? "" : comment);
        taskService.delegateTask(id, userId);
        taskService.setOwner(id, securityUtil.getCurrUser().getId());
        // ???????????????
        messageUtil.sendActMessage(userId, ActivitiConstant.MESSAGE_DELEGATE_CONTENT, sendMessage, sendSms, sendEmail);
        return ResultUtil.success("????????????");
    }

    @RequestMapping(value = "/deleteHistoric", method = RequestMethod.POST)
    @ApiOperation(value = "??????????????????")
    public Result<Object> deleteHistoric(@ApiParam("??????id") @RequestParam String[] ids) {

        for (String id : ids) {
            historyService.deleteHistoricTaskInstance(id);
        }
        return ResultUtil.success("????????????");
    }

    public class JumpTask implements Command<ExecutionEntity> {

        private String procInstId;
        private String activityId;

        public JumpTask(String procInstId, String activityId) {
            this.procInstId = procInstId;
            this.activityId = activityId;
        }

        @Override
        public ExecutionEntity execute(CommandContext commandContext) {

            ExecutionEntity executionEntity = commandContext.getExecutionEntityManager().findExecutionById(procInstId);
            executionEntity.destroyScope(ActivitiConstant.BACKED_FLAG);
            ProcessDefinitionImpl processDefinition = executionEntity.getProcessDefinition();
            ActivityImpl activity = processDefinition.findActivity(activityId);
            executionEntity.executeActivity(activity);

            return executionEntity;
        }
    }
}

package com.scmt.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmt.core.common.vo.PageVo;
import com.scmt.core.common.vo.Result;
import com.scmt.core.common.vo.SearchVo;
import com.scmt.healthy.entity.RelationProjectCritical;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author
 **/
public interface IRelationProjectCriticalService extends IService<RelationProjectCritical> {


    /**
     * 功能描述：实现分页查询
     *
     * @param relationProjectCritical 需要模糊查询的信息
     * @param searchVo                排序参数
     * @param pageVo                  分页参数
     * @return 返回获取结果
     */
    public Result<Object> queryRelationProjectCriticalListByPage(RelationProjectCritical relationProjectCritical, SearchVo searchVo, PageVo pageVo);

    /**
     * 功能描述： 导出
     *
     * @param relationProjectCritical 查询参数
     * @param response                response参数
     */
    public void download(RelationProjectCritical relationProjectCritical, HttpServletResponse response);

    List<RelationProjectCritical> queryRelationProjectCriticalList(RelationProjectCritical relationProjectCritical);

}

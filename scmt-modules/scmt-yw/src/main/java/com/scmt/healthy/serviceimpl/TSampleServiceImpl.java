package com.scmt.healthy.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmt.core.common.utils.ResultUtil;
import com.scmt.core.common.vo.PageVo;
import com.scmt.core.common.vo.Result;
import com.scmt.core.common.vo.SearchVo;
import com.scmt.core.utis.FileUtil;
import com.scmt.healthy.entity.TSample;
import com.scmt.healthy.mapper.TSampleMapper;
import com.scmt.healthy.service.ITSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author
 **/
@Service
public class TSampleServiceImpl extends ServiceImpl<TSampleMapper, TSample> implements ITSampleService {
    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    private TSampleMapper tSampleMapper;

    @Override
    public IPage<TSample> queryTSampleListByPage(TSample tSample, SearchVo searchVo, PageVo pageVo) {
        int page = 1;
        int limit = 10;
        if (pageVo != null) {
            if (pageVo.getPageNumber() != 0) {
                page = pageVo.getPageNumber();
            }
            if (pageVo.getPageSize() != 0) {
                limit = pageVo.getPageSize();
            }
        }
        Page<TSample> pageData = new Page<>(page, limit);
        QueryWrapper<TSample> queryWrapper = new QueryWrapper<>();
        if (tSample != null) {
            queryWrapper = LikeAllFeild(tSample, searchVo);
        }
        queryWrapper.orderByAsc("order_num");
        return tSampleMapper.selectPage(pageData, queryWrapper);
    }

    @Override
    public void download(TSample tSample, HttpServletResponse response) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        QueryWrapper<TSample> queryWrapper = new QueryWrapper<>();
        if (tSample != null) {
            queryWrapper = LikeAllFeild(tSample, null);
        }
        List<TSample> list = tSampleMapper.selectList(queryWrapper);
        for (TSample re : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("??????", re.getName());
            map.put("????????????", re.getBarcodeNum());
            map.put("??????", re.getSpec());
            map.put("?????????????????????0?????????1??????", re.getNeedTakeBlood());
            map.put("????????????", re.getCode());
            map.put("??????", re.getOrderNum());
            map.put("????????????", re.getIsPrint());
            map.put("?????????", re.getLiscode());
            map.put("??????", re.getCapacity());
            mapList.add(map);
        }
        FileUtil.createExcel(mapList, "exel.xlsx", response);
    }

    @Override
    public List<TSample> queryAllTSampleList(TSample tSample) {
        QueryWrapper<TSample> queryWrapper = new QueryWrapper<>();
        if (tSample != null) {
            queryWrapper = LikeAllFeild(tSample, null);
        }
        List<TSample> list = tSampleMapper.selectList(queryWrapper);
        return list;
    }

    /**
     * ?????????????????????????????????
     *
     * @param tSample ???????????????????????????
     * @return ????????????
     */
    public QueryWrapper<TSample> LikeAllFeild(TSample tSample, SearchVo searchVo) {
        QueryWrapper<TSample> queryWrapper = new QueryWrapper<>();
        if (tSample.getId() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getId, tSample.getId()));
        }
        if (tSample.getName() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getName, tSample.getName()));
        }
        if (tSample.getBarcodeNum() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getBarcodeNum, tSample.getBarcodeNum()));
        }
        if (tSample.getSpec() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getSpec, tSample.getSpec()));
        }
        if (tSample.getNeedTakeBlood() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getNeedTakeBlood, tSample.getNeedTakeBlood()));
        }
        if (tSample.getCode() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getCode, tSample.getCode()));
        }
        if (tSample.getOrderNum() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getOrderNum, tSample.getOrderNum()));
        }
        if (tSample.getIsPrint() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getIsPrint, tSample.getIsPrint()));
        }
        if (tSample.getLiscode() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getLiscode, tSample.getLiscode()));
        }
        if (tSample.getCapacity() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getCapacity, tSample.getCapacity()));
        }
        if (tSample.getDelFlag() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getDelFlag, tSample.getDelFlag()));
        }
        if (tSample.getCreateId() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getCreateId, tSample.getCreateId()));
        }
        if (tSample.getCreateTime() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getCreateTime, tSample.getCreateTime()));
        }
        if (tSample.getUpdateId() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getUpdateId, tSample.getUpdateId()));
        }
        if (tSample.getUpdateTime() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getUpdateTime, tSample.getUpdateTime()));
        }
        if (tSample.getDeleteId() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getDeleteId, tSample.getDeleteId()));
        }
        if (tSample.getDeleteTime() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getDeleteTime, tSample.getDeleteTime()));
        }
        if (tSample.getDepartmentId() != null) {
            queryWrapper.lambda().and(i -> i.like(TSample::getDepartmentId, tSample.getDepartmentId()));
        }
        if (searchVo != null) {
            if (searchVo.getStartDate() != null && searchVo.getEndDate() != null) {
                queryWrapper.lambda().and(i -> i.between(TSample::getCreateTime, searchVo.getStartDate(), searchVo.getEndDate()));
            }
        }
        queryWrapper.lambda().and(i -> i.eq(TSample::getDelFlag, 0));
        return queryWrapper;

    }
}

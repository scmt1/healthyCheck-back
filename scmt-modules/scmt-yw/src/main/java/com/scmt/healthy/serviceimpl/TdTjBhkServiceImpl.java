package com.scmt.healthy.serviceimpl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmt.core.common.vo.PageVo;
import com.scmt.core.common.vo.SearchVo;
import com.scmt.core.utis.FileUtil;
import com.scmt.healthy.entity.TdTjBhk;
import com.scmt.healthy.mapper.TdTjBhkMapper;
import com.scmt.healthy.service.ITdTjBhkService;
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
@DS("sub")
public class TdTjBhkServiceImpl extends ServiceImpl<TdTjBhkMapper, TdTjBhk> implements ITdTjBhkService {
    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    private TdTjBhkMapper tdTjBhkMapper;

    @Override
    public IPage<TdTjBhk> queryTdTjBhkListByPage(TdTjBhk tdTjBhk, SearchVo searchVo, PageVo pageVo) {
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
        Page<TdTjBhk> pageData = new Page<>(page, limit);
        QueryWrapper<TdTjBhk> queryWrapper = new QueryWrapper<>();
        if (tdTjBhk != null) {
            queryWrapper = LikeAllField(tdTjBhk, searchVo);
        }
        IPage<TdTjBhk> result = tdTjBhkMapper.selectPage(pageData, queryWrapper);
        return result;
    }

    @Override
    public void download(TdTjBhk tdTjBhk, HttpServletResponse response) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        QueryWrapper<TdTjBhk> queryWrapper = new QueryWrapper<>();
        if (tdTjBhk != null) {
            queryWrapper = LikeAllField(tdTjBhk, null);
        }
        List<TdTjBhk> list = tdTjBhkMapper.selectList(queryWrapper);
        for (TdTjBhk re : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("????????????id", re.getId());
            map.put("??????????????????", re.getRid());
            map.put("???????????????????????????????????????????????????????????????", re.getBhkorganCode());
            map.put("?????????????????????????????????", re.getBhkCode());
            map.put("??????????????????", re.getInstitutionCode());
            map.put("????????????", re.getCrptName());
            map.put("??????????????????", re.getCrptAddr());
            map.put("????????????", re.getPersonName());
            map.put("??????", re.getSex());
            map.put("????????????", re.getIdc());
            map.put("????????????", re.getBrth());
            map.put("??????", re.getAge());
            map.put("??????", re.getIsxmrd());
            map.put("??????????????????", re.getLnktel());
            map.put("????????????????????????", re.getDpt());
            map.put("????????????", re.getWrknum());
            map.put("???????????????", re.getWrklnt());
            map.put("???????????????,????????????12,????????????????????????????????????????????????", re.getWrklntmonth());
            map.put("??????????????????,????????????12", re.getTchbadrsntim());
            map.put("??????????????????", re.getTchbadrsnmonth());
            map.put("??????????????????", re.getWorkName());
            map.put("??????????????????", re.getOnguardState());
            map.put("????????????", re.getBhkDate());
            map.put("????????????", re.getBhkrst());
            map.put("????????????", re.getMhkadv());
            map.put("????????????", re.getVerdict());
            map.put("????????????", re.getMhkdct());
            map.put("??????????????????", re.getBhkType());
            map.put("??????????????????", re.getJdgdat());
            map.put("????????????", re.getBadrsn());
            map.put("???????????????", re.getIfRhk());
            map.put("?????????????????????????????????", re.getLastBhkCode());
            map.put("????????????????????????", re.getIdCardTypeCode());
            map.put("????????????", re.getWorkTypeCode());
            map.put("??????????????????", re.getHarmStartDate());
            map.put("????????????", re.getJcType());
            map.put("??????????????????", re.getRptPrintDate());
            map.put("??????????????????????????????", re.getCreditCodeEmp());
            map.put("??????????????????", re.getCrptNameEmp());
            map.put("??????????????????????????????", re.getIndusTypeCodeEmp());
            map.put("??????????????????????????????", re.getEconomyCodeEmp());
            map.put("??????????????????????????????", re.getCrptSizeCodeEmp());
            map.put("??????????????????????????????", re.getZoneCodeEmp());
            map.put("?????????????????? ??????????????????--0???????????? 1??????????????? 2???????????????", re.getFlag());
            mapList.add(map);
        }
        FileUtil.createExcel(mapList, "exel.xlsx", response);
    }

    @Override
    public List<Map<String, Object>> queryCompanyList() {
        return tdTjBhkMapper.queryCompanyList();
    }

    /**
     * ?????????????????????????????????
     *
     * @param tdTjBhk ???????????????????????????
     * @return ????????????
     */
    public QueryWrapper<TdTjBhk> LikeAllField(TdTjBhk tdTjBhk, SearchVo searchVo) {
        QueryWrapper<TdTjBhk> queryWrapper = new QueryWrapper<>();
        if (tdTjBhk.getId() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getId, tdTjBhk.getId()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getRid())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getRid, tdTjBhk.getRid()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getBhkorganCode())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getBhkorganCode, tdTjBhk.getBhkorganCode()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getBhkCode())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getBhkCode, tdTjBhk.getBhkCode()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getInstitutionCode())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getInstitutionCode, tdTjBhk.getInstitutionCode()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getCrptName())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getCrptName, tdTjBhk.getCrptName()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getCrptAddr())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getCrptAddr, tdTjBhk.getCrptAddr()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getPersonName())) {
            queryWrapper.lambda().and(i -> i.like(TdTjBhk::getPersonName, tdTjBhk.getPersonName()));
        }
        if (tdTjBhk.getSex() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getSex, tdTjBhk.getSex()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getIdc())) {
            queryWrapper.lambda().and(i -> i.like(TdTjBhk::getIdc, tdTjBhk.getIdc()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getBrth())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getBrth, tdTjBhk.getBrth()));
        }
        if (tdTjBhk.getAge() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getAge, tdTjBhk.getAge()));
        }
        if (tdTjBhk.getIsxmrd() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getIsxmrd, tdTjBhk.getIsxmrd()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getLnktel())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getLnktel, tdTjBhk.getLnktel()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getDpt())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getDpt, tdTjBhk.getDpt()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getWrknum())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getWrknum, tdTjBhk.getWrknum()));
        }
        if (tdTjBhk.getWrklnt() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getWrklnt, tdTjBhk.getWrklnt()));
        }
        if (tdTjBhk.getWrklntmonth() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getWrklntmonth, tdTjBhk.getWrklntmonth()));
        }
        if (tdTjBhk.getTchbadrsntim() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getTchbadrsntim, tdTjBhk.getTchbadrsntim()));
        }
        if (tdTjBhk.getTchbadrsnmonth() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getTchbadrsnmonth, tdTjBhk.getTchbadrsnmonth()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getWorkName())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getWorkName, tdTjBhk.getWorkName()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getOnguardState())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getOnguardState, tdTjBhk.getOnguardState()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getBhkDate())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getBhkDate, tdTjBhk.getBhkDate()));
        }
        if (tdTjBhk.getBhkrst() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getBhkrst, tdTjBhk.getBhkrst()));
        }
        if (tdTjBhk.getMhkadv() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getMhkadv, tdTjBhk.getMhkadv()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getVerdict())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getVerdict, tdTjBhk.getVerdict()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getMhkdct())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getMhkdct, tdTjBhk.getMhkdct()));
        }
        if (tdTjBhk.getBhkType() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getBhkType, tdTjBhk.getBhkType()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getJdgdat())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getJdgdat, tdTjBhk.getJdgdat()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getBadrsn())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getBadrsn, tdTjBhk.getBadrsn()));
        }
        if (tdTjBhk.getIfRhk() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getIfRhk, tdTjBhk.getIfRhk()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getLastBhkCode())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getLastBhkCode, tdTjBhk.getLastBhkCode()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getIdCardTypeCode())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getIdCardTypeCode, tdTjBhk.getIdCardTypeCode()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getWorkTypeCode())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getWorkTypeCode, tdTjBhk.getWorkTypeCode()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getHarmStartDate())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getHarmStartDate, tdTjBhk.getHarmStartDate()));
        }
        if (tdTjBhk.getJcType() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getJcType, tdTjBhk.getJcType()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getRptPrintDate())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getRptPrintDate, tdTjBhk.getRptPrintDate()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getCreditCodeEmp())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getCreditCodeEmp, tdTjBhk.getCreditCodeEmp()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getCrptNameEmp())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getCrptNameEmp, tdTjBhk.getCrptNameEmp()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getIndusTypeCodeEmp())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getIndusTypeCodeEmp, tdTjBhk.getIndusTypeCodeEmp()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getEconomyCodeEmp())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getEconomyCodeEmp, tdTjBhk.getEconomyCodeEmp()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getCrptSizeCodeEmp())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getCrptSizeCodeEmp, tdTjBhk.getCrptSizeCodeEmp()));
        }
        if (StringUtils.isNotBlank(tdTjBhk.getZoneCodeEmp())) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getZoneCodeEmp, tdTjBhk.getZoneCodeEmp()));
        }
        if (tdTjBhk.getFlag() != null) {
            queryWrapper.lambda().and(i -> i.eq(TdTjBhk::getFlag, tdTjBhk.getFlag()));
        }
        if (searchVo != null) {
            if (searchVo.getStartDate() != null && searchVo.getEndDate() != null) {
                queryWrapper.lambda().and(i -> i.between(TdTjBhk::getBhkDate, searchVo.getStartDate(), searchVo.getEndDate()));
            }
        }
        return queryWrapper;
    }
}
package com.scmt.healthy.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.scmt.healthy.entity.TGroupUnit;
import com.scmt.healthy.service.ITGroupUnitService;
import com.scmt.core.common.vo.PageVo;
import com.scmt.core.common.vo.SearchVo;
import com.scmt.healthy.mapper.TGroupUnitMapper;
import com.scmt.core.utis.FileUtil;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
/**
 *@author
 **/
@Service
public class TGroupUnitServiceImpl extends ServiceImpl<TGroupUnitMapper, TGroupUnit> implements ITGroupUnitService {
	@Autowired
	@SuppressWarnings("SpringJavaAutowiringInspection")
	private TGroupUnitMapper tGroupUnitMapper;

	@Override
	public IPage<TGroupUnit> queryTGroupUnitListByPage(TGroupUnit  tGroupUnit, SearchVo searchVo, PageVo pageVo){
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
		Page<TGroupUnit> pageData = new Page<>(page, limit);
		QueryWrapper<TGroupUnit> queryWrapper = new QueryWrapper<>();
		if (tGroupUnit !=null) {
			queryWrapper = LikeAllFeild(tGroupUnit,searchVo);
		}
		IPage<TGroupUnit> result = tGroupUnitMapper.selectPage(pageData, queryWrapper);
		return  result;
	}
	@Override
	public void download(TGroupUnit tGroupUnit, HttpServletResponse response) {
		List<Map<String, Object>> mapList = new ArrayList<>();
		QueryWrapper<TGroupUnit> queryWrapper = new QueryWrapper<>();
		if (tGroupUnit !=null) {
			queryWrapper = LikeAllFeild(tGroupUnit,null);
		}
		List<TGroupUnit> list = tGroupUnitMapper.selectList(queryWrapper);
		for (TGroupUnit re : list) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("????????????", re.getName());
			map.put("????????????????????????", re.getUscc());
			map.put("????????????", re.getLegalPerson());
			map.put("????????????????????????", re.getRegCapital());
			map.put("????????????", re.getEstablishmentDate());
			map.put("????????????", re.getIndustryCode());
			map.put("????????????", re.getBusinessScaleCode());
			map.put("????????????", re.getEconomicTypeCode());
			map.put("??????????????????", re.getAddress());
			map.put("????????????", re.getAttachment());
			map.put("??????????????????", re.getIndustryName());
			map.put("??????????????????", re.getBusinessScaleName());
			map.put("??????????????????", re.getEconomicTypeName());
			map.put("??????????????????", re.getRegionCode());
			map.put("??????????????????", re.getRegionName());
			map.put("????????????", re.getEmployeesNum());
			map.put("?????????????????????????????????", re.getDangerNum());
			map.put("????????????", re.getLegalPhone());
			map.put("???????????????", re.getWorkmanNum());
			map.put("???????????????????????????????????????", re.getWorkmistressNum());
			map.put("??????????????????", re.getPostalCode());
			map.put("????????????", re.getWorkArea());
			map.put("????????????", re.getFilingDate());
			map.put("???????????????", re.getLinkMan1());
			map.put("?????????????????????", re.getPosition1());
			map.put("??????????????????", re.getLinkPhone1());
			map.put("???????????????", re.getLinkMan2());
			map.put("?????????????????????", re.getPosition2());
			map.put("?????????????????????", re.getLinkPhone2());
			map.put("???????????????????????????", re.getSafetyPrincipal());
			map.put("?????????????????????", re.getSafePosition());
			map.put("?????????????????????", re.getSafePhone());
			map.put("????????????", re.getSubjeConn());
			map.put("??????????????????", re.getEnrolAddress());
			map.put("??????????????????", re.getEnrolPostalCode());
			map.put("????????????????????????", re.getOccManaOffice());
			mapList.add(map);
		}
		FileUtil.createExcel(mapList, "exel.xlsx", response);
	}

	/**
	* ?????????????????????????????????
	* @param tGroupUnit ???????????????????????????
	* @return ????????????
	*/
	public QueryWrapper<TGroupUnit>  LikeAllFeild(TGroupUnit  tGroupUnit, SearchVo searchVo) {
		QueryWrapper<TGroupUnit> queryWrapper = new QueryWrapper<>();
		if(StringUtils.isNotBlank(tGroupUnit.getId())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getId, tGroupUnit.getId()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getName())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getName, tGroupUnit.getName()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getPhysicalType())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getPhysicalType, tGroupUnit.getPhysicalType()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getUscc())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getUscc, tGroupUnit.getUscc()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getLegalPerson())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getLegalPerson, tGroupUnit.getLegalPerson()));
		}
		if(tGroupUnit.getRegCapital() != null){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getRegCapital, tGroupUnit.getRegCapital()));
		}
		if(tGroupUnit.getEstablishmentDate() != null){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getEstablishmentDate, tGroupUnit.getEstablishmentDate()));
		}

		if(StringUtils.isNotBlank(tGroupUnit.getIndustryCode())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getIndustryCode, tGroupUnit.getIndustryCode()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getBusinessScaleCode())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getBusinessScaleCode, tGroupUnit.getBusinessScaleCode()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getEconomicTypeCode())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getEconomicTypeCode, tGroupUnit.getEconomicTypeCode()));
		}

		if(StringUtils.isNotBlank(tGroupUnit.getAddress())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getAddress, tGroupUnit.getAddress()));
		}

		if(StringUtils.isNotBlank(tGroupUnit.getDepartmentId())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getDepartmentId, tGroupUnit.getDepartmentId()));
		}

		if(StringUtils.isNotBlank(tGroupUnit.getCreateId())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getCreateId, tGroupUnit.getCreateId()));
		}
		if(tGroupUnit.getCreateTime() != null){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getCreateTime, tGroupUnit.getCreateTime()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getUpdateId())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getUpdateId, tGroupUnit.getUpdateId()));
		}
		if(tGroupUnit.getUpdateTime() != null){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getUpdateTime, tGroupUnit.getUpdateTime()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getDeleteId())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getDeleteId, tGroupUnit.getDeleteId()));
		}
		if(tGroupUnit.getDeleteTime() != null){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getDeleteTime, tGroupUnit.getDeleteTime()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getLinkMan2())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getLinkMan2, tGroupUnit.getLinkMan2()));
		}
		if(StringUtils.isNotBlank(tGroupUnit.getLinkPhone2())){
			queryWrapper.lambda().and(i -> i.like(TGroupUnit::getLinkPhone2, tGroupUnit.getLinkPhone2()));
		}
		if(searchVo!=null){
			if(searchVo.getStartDate()!=null && searchVo.getEndDate()!=null){
				queryWrapper.lambda().and(i -> i.between(TGroupUnit::getCreateTime, searchVo.getStartDate(),searchVo.getEndDate()));
			}
		}
		queryWrapper.lambda().and(i -> i.eq(TGroupUnit::getDelFlag, 0));
		return queryWrapper;

}
}

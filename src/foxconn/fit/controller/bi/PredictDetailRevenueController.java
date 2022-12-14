package foxconn.fit.controller.bi;

import foxconn.fit.advice.Log;
import foxconn.fit.controller.BaseController;
import foxconn.fit.entity.base.AjaxResult;
import foxconn.fit.entity.budget.PredictDetailRevenue;
import foxconn.fit.service.budget.ForecastDetailRevenueSrcService;
import foxconn.fit.service.budget.PredictDetailRevenueService;
import foxconn.fit.util.ExceptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.util.WebUtils;
import org.springside.modules.orm.Page;
import org.springside.modules.orm.PageRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
@Controller
@RequestMapping("/bi/predictDetailRevenue")
public class PredictDetailRevenueController extends BaseController {

	@Autowired
	private PredictDetailRevenueService forecastRevenueService;
	
	@Autowired
	private ForecastDetailRevenueSrcService forecastDetailRevenueSrcService;

	@RequestMapping(value = "index")
	public String index(Model model,HttpServletRequest request) {
		model=forecastRevenueService.index(model);
		return "/bi/predictDetailRevenue/index";
	}

	@RequestMapping(value="/list")
	public String list(Model model,HttpServletRequest request,PageRequest pageRequest,String entity,String year,String version) {
		try {
			String sql=forecastRevenueService.list(year,version,entity);
			Page<Object[]> page = forecastRevenueService.findPageBySql(pageRequest, sql, PredictDetailRevenue.class);
			model.addAttribute("page", page);
			model.addAttribute("year", year.substring(2));
		} catch (Exception e) {
			logger.error("????????????????????????????????????:", e);
		}
		return "/bi/predictDetailRevenue/list";
	}

	@RequestMapping(value="/delete")
	@ResponseBody
	public String delete(HttpServletRequest request,AjaxResult ajaxResult,Model model,String id){
		Locale locale = (Locale) WebUtils.getSessionAttribute(request,SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
		ajaxResult.put("msg", getLanguage(locale, "????????????", "Delete Success"));
		try {
			Assert.hasText(id, getLanguage(locale, "ID????????????", "Id can not be null"));
			forecastRevenueService.delete(id);
		} catch (Exception e) {
			logger.error("????????????:", e);
			ajaxResult.put("flag", "fail");
			ajaxResult.put("msg", getLanguage(locale, "????????????", "Delete Fail")+ " : " + ExceptionUtil.getRootCauseMessage(e));
		}
		
		return ajaxResult.getJson();
	}
	
	@RequestMapping(value = "upload")
	@ResponseBody
	@Log(name = "??????????????????-->??????")
	public String upload(HttpServletRequest request,HttpServletResponse response, AjaxResult result) {
		Locale locale = (Locale) WebUtils.getSessionAttribute(request,SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
		result.put("msg", getLanguage(locale, "????????????", "Upload Success"));
		MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
		return forecastRevenueService.upload(result,locale,multipartHttpServletRequest);
	}

	
	@RequestMapping(value = "download")
	@ResponseBody
	@Log(name = "??????????????????-->??????")
	public synchronized String download(HttpServletRequest request,HttpServletResponse response,PageRequest pageRequest,AjaxResult result,
			@Log(name = "SBU") String entitys,@Log(name = "???") String year,@Log(name = "??????") String version){
		try {
			Locale locale = (Locale) WebUtils.getSessionAttribute(request,SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
			Assert.hasText(year, getLanguage(locale, "???????????????", "Year can not be null"));
			Assert.hasText(entitys, getLanguage(locale, "SBU????????????", "SBU can not be null"));
			Map<String,String> map=forecastRevenueService.download(entitys,year,version,request,pageRequest);
			if(map.get("result").equals("Y")){
				result.put("fileName", map.get("file"));
			}else{
				result.put("flag", "fail");
				result.put("msg", getLanguage(locale, "????????????????????????", "Fail to download template file") + " : " + map.get("str"));
			}
		} catch (Exception e) {
			logger.error("??????Excel??????", e);
			result.put("flag", "fail");
			result.put("msg", ExceptionUtil.getRootCauseMessage(e));
		}

		return result.getJson();
	}

	/**
	 * ???????????????
	 */
	@RequestMapping(value = "dimension")
	@ResponseBody
	public synchronized String dimension(HttpServletRequest request, HttpServletResponse response, AjaxResult result) {
		Locale locale = (Locale) WebUtils.getSessionAttribute(request, SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
		 Map<String,String> map=forecastDetailRevenueSrcService.dimension(request);
			if(map.get("result")=="Y"){
				result.put("fileName", map.get("str"));
			}else{
				result.put("flag", "fail");
				result.put("msg", getLanguage(locale, "????????????????????????", "Fail to download template file") + " : " + map.get("str"));
			}
		return result.getJson();
	}


	/**
	 * ????????????
	 */
	@RequestMapping(value = "template")
	@ResponseBody
	public synchronized String template(HttpServletRequest request, HttpServletResponse response, AjaxResult result) {
		Locale locale = (Locale) WebUtils.getSessionAttribute(request, SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
		Map<String,String> map=forecastRevenueService.template(request);
		if(map.get("result")=="Y"){
			result.put("fileName", map.get("file"));
		}else{
			result.put("flag", "fail");
			result.put("msg", getLanguage(locale, "????????????????????????", "Fail to download template file") + " : " + map.get("str"));
		}
		return result.getJson();
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = "version")
	@ResponseBody
	public synchronized String version(HttpServletRequest request, HttpServletResponse response, AjaxResult result) {
		String version=forecastRevenueService.version();
		result.put("version", version);
		return result.getJson();
	}
}

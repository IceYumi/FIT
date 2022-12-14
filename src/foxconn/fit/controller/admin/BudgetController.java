package foxconn.fit.controller.admin;

import com.csvreader.CsvWriter;
import foxconn.fit.advice.Log;
import foxconn.fit.controller.BaseController;
import foxconn.fit.entity.base.AjaxResult;
import foxconn.fit.entity.base.EnumDimensionType;
import foxconn.fit.service.base.BudgetService;
import foxconn.fit.service.base.UserService;
import foxconn.fit.util.ExceptionUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springside.modules.orm.PageRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bi/budget")
public class BudgetController extends BaseController {

	@Autowired
	private BudgetService budgetService;
	
//	@Autowired
//	private UserService userService;
//
//	@RequestMapping(value = "index")
//	public String index(Model model) {
//		List<String> yearsList = userService.listBySql("select distinct dimension from FIT_DIMENSION where type='"+EnumDimensionType.Years.getCode()+"' order by dimension");
//		List<String> sbuList = userService.listBySql("select distinct parent from fit_dimension where type='"+EnumDimensionType.Entity+"' order by parent");
//		model.addAttribute("yearsList", yearsList);
//		model.addAttribute("sbuList", sbuList);
//		return "/admin/budget/index";
//	}


	@RequestMapping(value = "download")
	@ResponseBody
	@Log(name = "下载Budget")
	public synchronized String download(HttpServletRequest request,HttpServletResponse response,PageRequest pageRequest,AjaxResult result,
			@Log(name = "SBU") String sbu,@Log(name = "年") String year){
		try {
			String[] years=year.split(",");
			List<Map> list =new ArrayList<>();
			for (int i=0;i<years.length;i++){
				String yr=years[i];
				if (sbu.endsWith(",")) {
					sbu=sbu.substring(0,sbu.length()-1);
				}
				String entity="";
				for (String s : sbu.split(",")) {
					entity+=s+"|";
				}
				entity=entity.substring(0,entity.length()-1);

				String message = budgetService.generatePlanning(entity,yr);
				if (StringUtils.isNotEmpty(message)) {
					throw new RuntimeException("计算Budget数据出错 : "+message);
				}
				String sql="select ACCOUNT,JAN,FEB, MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC,YT,POINT_OF_VIEW,DATA_LOAD_CUBE_NAME from CUX_FIT_PLANNING_V order by POINT_OF_VIEW";
				List<Map> lists = budgetService.listMapBySql(sql);
				list.addAll(lists);
			};

			String realPath = request.getRealPath("");
			if (CollectionUtils.isNotEmpty(list)) {
				long time = System.currentTimeMillis();
				String filePath=realPath+File.separator+"static"+File.separator+"download"+File.separator+time+".csv";
				CsvWriter writer=new CsvWriter(filePath, ',', Charset.forName("UTF8"));
				writer.writeRecord(new String[]{"Account","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec","YT","Point-of-View","Data Load Cube Name"});
				for (Map map : list) {
					if(null==map.get("DATA_LOAD_CUBE_NAME")){
						writer.writeRecord(new String[]{map.get("ACCOUNT").toString(),
						judgement(map.get("JAN").toString()),judgement(map.get("FEB").toString()),judgement(map.get("MAR").toString()),judgement(map.get("APR").toString()),
						judgement(map.get("MAY").toString()),judgement(map.get("JUN").toString()),judgement(map.get("JUL").toString()),judgement(map.get("AUG").toString()),
						judgement(map.get("SEP").toString()),judgement(map.get("OCT").toString()),judgement(map.get("NOV").toString()),judgement(map.get("DEC").toString()),
						map.get("YT").toString(),map.get("POINT_OF_VIEW").toString(),""});
					}else{
						writer.writeRecord(new String[]{map.get("ACCOUNT").toString(),
								judgement(map.get("JAN").toString()),judgement(map.get("FEB").toString()),judgement(map.get("MAR").toString()),judgement(map.get("APR").toString()),
								judgement(map.get("MAY").toString()),judgement(map.get("JUN").toString()),judgement(map.get("JUL").toString()),judgement(map.get("AUG").toString()),
								judgement(map.get("SEP").toString()),judgement(map.get("OCT").toString()),judgement(map.get("NOV").toString()),judgement(map.get("DEC").toString()),
								map.get("YT").toString(),map.get("POINT_OF_VIEW").toString(),map.get("DATA_LOAD_CUBE_NAME").toString()});
					}
				}

				writer.flush();
				writer.close();
				result.put("fileName", time+".csv");
				System.gc();
			}else {
				result.put("flag", "fail");
				result.put("msg", "没有查询到可下载的数据(No data can be downloaded)");
			}
		} catch (Exception e) {
			logger.error("下载Excel失败", e);
			result.put("flag", "fail");
			result.put("msg", ExceptionUtil.getRootCauseMessage(e));
		}

		return result.getJson();
	}

	public String judgement(String val){
		if(val.equals("0")){
			return "";
		}
		return val;
	}

}

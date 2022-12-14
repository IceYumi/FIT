package foxconn.fit.controller.bi;

import foxconn.fit.advice.Log;
import foxconn.fit.controller.BaseController;
import foxconn.fit.entity.base.AjaxResult;
import foxconn.fit.entity.base.EnumGenerateType;
import foxconn.fit.entity.bi.PoColumns;
import foxconn.fit.entity.bi.PoKey;
import foxconn.fit.entity.bi.PoTable;
import foxconn.fit.service.base.UserDetailImpl;
import foxconn.fit.service.bi.PoCenterService;
import foxconn.fit.service.bi.PoTableService;
import foxconn.fit.util.DateUtil;
import foxconn.fit.util.ExcelUtil;
import foxconn.fit.util.ExceptionUtil;
import foxconn.fit.util.SecurityUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.util.WebUtils;
import org.springside.modules.orm.Page;
import org.springside.modules.orm.PageRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/bi/poIntegration")
public class PoIntegrationController extends BaseController {

    @Autowired
    private PoTableService poTableService;
    @Autowired
    private PoCenterService poCenterService;

    @RequestMapping(value = "index")
    public String index(PageRequest pageRequest, Model model, HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if(String.valueOf(session.getAttribute("detailsTsak")).equalsIgnoreCase("ok")){
                List<String> listSBU=poTableService.listBySql(" select SBU from FIT_PO_SBU_YEAR_CD_SUM where TASK_ID='"+session.getAttribute("taskId")+"'" +
                        "  and rownum=1 ");
                List<String> listYear=poTableService.listBySql(" select year from FIT_PO_SBU_YEAR_CD_SUM where TASK_ID='"+session.getAttribute("taskId")+"'" +
                        "  and rownum=1 ");
                String sbuVal=listSBU.get(0);
                String DateYear=listYear.get(0);
                model.addAttribute("sbuVal",sbuVal);
                model.addAttribute("DateYear",DateYear);
            }
            Locale locale = (Locale) WebUtils.getSessionAttribute(request, SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
            UserDetailImpl loginUser = SecurityUtils.getLoginUser();
            String userName = loginUser.getUsername();
            String permSql = "  select listagg(r.TABLE_PERMS, ',') within GROUP(ORDER BY r.id) as TABLE_PERMS  from  fit_user u \n" +
                    " left join FIT_PO_AUDIT_ROLE_USER ur on u.id=ur.user_id \n" +
                    " left join FIT_PO_AUDIT_ROLE r on ur.role_id=r.id\n" +
                    " where u.username=" + "'" + userName + "'";
            List<String> perms = poTableService.listBySql(permSql);
            String roleSql = " select distinct r.code  from  fit_user u \n" +
                    " left join FIT_PO_AUDIT_ROLE_USER ur on u.id=ur.user_id \n" +
                    " left join FIT_PO_AUDIT_ROLE r on ur.role_id=r.id\n" +
                    " WHERE r.grade='1' and u.username='" + userName + "'";
            String keyUserSql = " select count(1)  from  fit_user u \n" +
                    " left join FIT_PO_AUDIT_ROLE_USER ur on u.id=ur.user_id \n" +
                    " left join FIT_PO_AUDIT_ROLE r on ur.role_id=r.id\n" +
                    " WHERE r.code='KEYUSER' and u.username='" + userName + "'";
            List<Map> maps = poTableService.listMapBySql(keyUserSql);
            List<String> roleCode = poTableService.listBySql(roleSql);
            //???????????? 1 ????????? ?????????????????? 2 ????????? sbu??????
            List<String> dataRange = new ArrayList<>();
            String hasKey = "0";
            String userSql = "";
            if (maps != null && !"0".equals(maps.get(0).get("COUNT(1)").toString())) {
                hasKey = "1";
            }
            if (null != roleCode && roleCode.size() > 0) {
                String roles = roleCode.get(0) == null ? "" : roleCode.get(0);
                if (roles.equalsIgnoreCase("MM")) {
                    userSql = "select SBU  from fit_user where username='" + userName + "' and sbu is not null " +
                            "union all " +
                            "select listagg(t.NEW_SBU_NAME, ',') within group(order by t.NEW_SBU_NAME) as sbu " +
                            "  from " +
                            "(select distinct tie.NEW_SBU_NAME from bidev.v_if_sbu_mapping tie " +
                            " where exists (select SBU  from fit_user where username='" + userName + "' and sbu is null) " +
                            " ) t ";
                    //select   distinct  SBU from EPMEBS.CUX_SBU_BU_MAPPING order by SBU
//                    select distinct tie.NEW_SBU_NAME from bidev.v_if_sbu_mapping tie order by tie.NEW_SBU_NAME

                } else if (roles.equalsIgnoreCase("SOURCER")) {
                    userSql = " select COMMODITY_MAJOR  from fit_user where username='" + userName + "' and COMMODITY_MAJOR is not null " +
                            "union all " +
                            "select listagg(t.COMMODITY_NAME, ',') within group(order by t.COMMODITY_NAME) as COMMODITY_MAJOR " +
                            "  from " +
                            "(select distinct tie.COMMODITY_NAME from CUX_FUNCTION_COMMODITY_MAPPING tie " +
                            " where exists (select COMMODITY_MAJOR  from fit_user where username='" + userName + "' and COMMODITY_MAJOR is null) " +
                            " ) t ";
                }
            }
            if (!userSql.equalsIgnoreCase("")) {
                List<String> ranges = poTableService.listBySql(userSql);
                String[] split = ranges.get(0).split(",");
                dataRange = Arrays.asList(split);
            }
            String whereSql = "";

            if (perms.get(0) != null && perms.get(0).length() != 0) {
                String perm = perms.get(0);
                String[] berSplit = perm.split(",");
                List list = Arrays.asList(berSplit);
                Set set = new HashSet(list);
                String[] split = (String[]) set.toArray(new String[0]);
                for (String s : split) {
                    whereSql += "'" + s + "',";
                }
            }
            if (whereSql.length() >= 1) {
                whereSql = whereSql.substring(0, whereSql.length() - 1);
            } else {
                whereSql = "'a'";
            }
//            String uploadSql = " select * from Fit_po_table where TYPE='PO' and Upload_flag='Y' " +
//                    "and table_name in (" + whereSql + ") order by serial";
            String uploadSql = "select a.* from FIT_PO_TABLE a,FIT_PO_BUTTON_ROLE b" +
                    " where a.table_name=b.form_name and type in('PO','CPO') and b.BUTTONS_TYPE=2 and b.role_id in (" + whereSql + ") order by serial";
//            String exportSql = " select * from Fit_po_table where TYPE='PO' and Upload_flag!='Y' order by serial";
            String exportSql = "select a.* from FIT_PO_TABLE a,FIT_PO_BUTTON_ROLE b" +
                    " where a.table_name=b.form_name and type in('PO','CPO') and b.BUTTONS_TYPE=1 and b.role_id in (" + whereSql + ") order by serial";
            String selectSql = "select a.* from FIT_PO_TABLE a,FIT_PO_BUTTON_ROLE b" +
                    " where a.table_name=b.form_name and type in('PO','CPO') and b.BUTTONS_TYPE=3 and b.role_id in (" + whereSql + ") order by serial";

            List<PoTable> poTableListSelect = poTableService.listBySql(selectSql, PoTable.class);
            List<PoTable> poTableList = poTableService.listBySql(uploadSql, PoTable.class);
            List<PoTable> poTableOutList = poTableService.listBySql(exportSql, PoTable.class);


            List<PoTable> tableList = new ArrayList<PoTable>();
            for (PoTable poTable : poTableList) {
                tableList.add(new PoTable(poTable.getTableName(), getByLocale(locale, poTable.getComments())));
            }

            List<PoTable> tableListSelect = new ArrayList<>();
            for (PoTable poTable : poTableListSelect) {
                tableListSelect.add(new PoTable(poTable.getTableName(), getByLocale(locale, poTable.getComments())));
            }

            List<PoTable> tableOutList = new ArrayList<PoTable>();
            for (PoTable poTable : poTableOutList) {
                if (poTable.getTableName().equalsIgnoreCase("FIT_PO_CD_MONTH_DTL")) {
                    tableOutList.add(new PoTable("FIT_PO_CD_MONTH_DOWN", getByLocale(locale, poTable.getComments())));
                } else if (!poTable.getTableName().equalsIgnoreCase("FIT_PO_CD_MONTH_DOWN")) {
                    tableOutList.add(new PoTable(poTable.getTableName(), getByLocale(locale, poTable.getComments())));
                }
            }

            List<String> commodityList=poTableService.listBySql("select COMMODITY_MAJOR  from BIDEV.v_dm_d_commodity_major order by FUNCTION_NAME");
            List<String> poCenters = poCenterService.findPoCenters();
            session.setAttribute("dataRange", dataRange);
            request.setAttribute("poCenters", poCenters);
            model.addAttribute("commodityList", commodityList);

            model.addAttribute("poTableList", tableList);
            model.addAttribute("poTableOutList", tableOutList);
            model.addAttribute("tableListSelect", tableListSelect);
            model.addAttribute("dataRange", dataRange);
            model.addAttribute("hasKey", hasKey);
            List<String> typeList = poTableService.listBySql("select distinct tablename from FIT_AUDIT_CONSOL_CONFIG order by tablename");
            model.addAttribute("typeList", typeList);
        } catch (Exception e) {
            logger.error("???????????????????????????????????????", e);
        }
        return "/bi/poIntegration/index";
    }


    @RequestMapping(value = "upload")
    @ResponseBody
    @Log(name = "????????????-->??????")
    public String upload(HttpServletRequest request, HttpServletResponse response, AjaxResult result, @Log(name = "???????????????") String[] tableNamesOut1) {
        Locale locale = (Locale) WebUtils.getSessionAttribute(request, SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
        result.put("msg", getLanguage(locale, "????????????", "Upload success"));
        UserDetailImpl loginUser = SecurityUtils.getLoginUser();
        List<String> subs = poTableService.listBySql("select distinct tie.NEW_SBU_NAME from bidev.v_if_sbu_mapping tie  where tie.NEW_SBU_NAME in('IDS','EMS','ABS','ACE','ASD','AEC','TSC','APS','CW','FAD','IoT','CIDA','Tengyang','TMTS','FIAD') order by tie.NEW_SBU_NAME");
        List<String> commoditys = poCenterService.findCommoditys();
        List<String> monthList = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String[] date =formatter.format(new Date()).split("-");
        String tableName = "";
        HttpSession session = request.getSession();
        String dataRangeStr = "";
        if (session != null) {
            dataRangeStr = session.getAttribute("dataRange").toString();
        } else {
            result.put("msg", "??????????????????????????????");
            return result.getJson();
        }
        try {
            if(!poTableService.updateState(loginUser.getUsername())){
                if("FIT_ACTUAL_PO_NPRICECD_DTL".equalsIgnoreCase(tableNamesOut1[0])||"FIT_PO_BUDGET_CD_DTL".equalsIgnoreCase(tableNamesOut1[0])){
                    if(Integer.parseInt(date[0])>10){
                        result.put("flag", "fail");
                        result.put("msg", getLanguage(locale, "?????????????????????1-10??????????????????????????????????????????", "The upload time is from the 1st to the 10th of each month, it is overdue, please contact the administrator"));
                        return result.getJson();
                    }
                }
            }
            Assert.isTrue(tableNamesOut1 != null && tableNamesOut1.length > 0, getLanguage(locale, "?????????????????????", "The table cannot be empty"));
            String year = date[2];
            String period = Integer.toString(Integer.valueOf(date[1])-1);
            List<String> sbuList = new ArrayList<>();
            List<String> commodityList = new ArrayList<>();
            Set<String> sbuSet = new HashSet<>();
            Set<String> commoditySet = new HashSet<>();
            String sbu = "";
            String commodity = "";
            if (period.length() < 2) {
                period = "0" + period;
            }
            MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
            Map<String, MultipartFile> mutipartFiles = multipartHttpServletRequest.getFileMap();
            if (mutipartFiles != null && mutipartFiles.size() > 0) {
                MultipartFile file = (MultipartFile) mutipartFiles.values().toArray()[0];
                String suffix = "";
                if (file.getOriginalFilename().lastIndexOf(".") != -1) {
                    suffix = file.getOriginalFilename().substring(
                            file.getOriginalFilename().lastIndexOf(".") + 1,
                            file.getOriginalFilename().length());
                    suffix = suffix.toLowerCase();
                }
                if (!"xls".equals(suffix) && !"xlsx".equals(suffix)) {
                    result.put("flag", "fail");
                    result.put("msg", getLanguage(locale, "????????????????????????Excel??????", "The format of excel is error"));
                    return result.getJson();
                }
                Workbook wb = null;
                if ("xls".equals(suffix)) {
                    //Excel2003
                    wb = new HSSFWorkbook(file.getInputStream());
                } else {
                    //Excel2007
                    wb = new XSSFWorkbook(file.getInputStream());
                }
                wb.close();
                Map<PoTable, List<List<String>>> dataMap = new HashMap<PoTable, List<List<String>>>();
                for (int i = 0; i < tableNamesOut1.length; i++) {
                    tableName = tableNamesOut1[i];
                    PoTable poTable = poTableService.get(tableName);
                    List<PoColumns> columns = poTable.getColumns();
                    List<PoKey> keys = poTable.getKeys();
                    int COLUMN_NUM = columns.size();
                    Sheet sheet = wb.getSheetAt(i);
                    Row firstRow = sheet.getRow(0);
                    Assert.notNull(firstRow, getLanguage(locale, "???" + (i + 1) + "???sheet??????????????????????????????????????????", "The title line of the " + (i + 1) + "th sheet cannot be empty"));
                    int columnNum = firstRow.getPhysicalNumberOfCells();
                    if (columnNum < COLUMN_NUM) {
                        result.put("flag", "fail");
                        result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet?????????????????????" + COLUMN_NUM, "The number of columns in sheet " + (i + 1) + " cannot be less than " + COLUMN_NUM));
                        return result.getJson();
                    }
                    int rowNum = sheet.getPhysicalNumberOfRows();
                    if (rowNum < 3) {
                        result.put("flag", "fail");
                        result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet????????????????????????", "Sheet " + (i + 1) + " does not fill in the data"));
                        return result.getJson();
                    }

                    List<List<String>> dataList = new ArrayList<List<String>>();
                    List<List<String>> dataList2 = new ArrayList<List<String>>();
                    List<Integer> keySerialList = new ArrayList<Integer>();
                    if (keys != null && keys.size() > 0) {
                        for (PoKey poKey : keys) {
                            keySerialList.add(poKey.getSerial());
                        }
                    }
                    Map<String, Integer> keyRepeatMap = new HashMap<String, Integer>();
                    for (int j = 2; j < rowNum; j++) {
                        Row row = sheet.getRow(j);
                        if (row == null) {
                            continue;
                        }

                        boolean isBlankRow = true;
                        for (int k = 0; k < COLUMN_NUM; k++) {
                            if (StringUtils.isNotEmpty(ExcelUtil.getCellStringValue(row.getCell(k), i, j))) {
                                isBlankRow = false;
                            }
                        }
                        if (isBlankRow) {
                            continue;
                        }
                        int n = 0;
                        List<String> data = new ArrayList<String>();
                        data.add(year);
                        String year_month = year + period;
                        String recordsYear = ExcelUtil.getCellStringValue(row.getCell(0), i, j);
                        String recordsMonth = ExcelUtil.getCellStringValue(row.getCell(1), i, j);
                        if (recordsMonth.length() < 2) {
                            recordsMonth = "0" + recordsMonth;
                        }
                        String RYM = recordsYear + recordsMonth;
                        int num = 3;
                        if ("FIT_PO_SBU_YEAR_CD_SUM".equalsIgnoreCase(tableName)) {
                            Assert.isTrue(year.equals(recordsYear), getLanguage(locale, "?????????????????? " + recordsYear + "?????????" + year, "The year is error:" + RYM + "should be???" + year));
                            // poCenterValue=ExcelUtil.getCellStringValue(row.getCell(1),i,j);
                            num = num - 1;
                            n += 1;
                        } else if ("FIT_PO_CD_MONTH_DTL".equalsIgnoreCase(tableName)) {
                            System.out.println("FIT_PO_CD_MONTH_DTL????????????");
                            n = 1;
                            Assert.isTrue(year.equals(recordsYear), getLanguage(locale, "?????????????????? " + recordsYear + "?????????" + year, "The year is error:" + RYM + "should be???" + year));
                        } else {
                            Assert.isTrue(year_month.equals(RYM), getLanguage(locale, "?????????????????? " + RYM + "?????????" + year_month, "The year???period is error:" + RYM + "should be???" + year_month));
                            data.add(period);
                            n += 2;
                        }

                        String key = null;
                        while (n < COLUMN_NUM) {
                            PoColumns column = columns.get(n);
                            if (keySerialList.contains(column.getSerial())) {
                                String value = ExcelUtil.getCellStringValue(row.getCell(n), i, j);
                                if (StringUtils.isEmpty(value)) {
                                    result.put("flag", "fail");
                                    result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "??????????????????????????????", "The contents of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " cannot be empty"));
                                    return result.getJson();
                                }
                                value = value.replaceAll("'", "''");
                                data.add(value);
                            } else {
                                if (column.getNullable() == false) {
                                    if (column.getDataType().equalsIgnoreCase("date")) {
                                        try {
                                            data.add(DateUtil.formatByddSMMSyyyy(ExcelUtil.getCellDateValue(row.getCell(n), DateUtil.SDF_ddSMMSyyyy)));
                                        } catch (Exception e) {
                                            result.put("flag", "fail");
                                            result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "?????????????????????", "The format of date in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error"));
                                            return result.getJson();
                                        }
                                    } else {
                                        String value = ExcelUtil.getCellStringValue(row.getCell(n), i, j);
                                        if (StringUtils.isEmpty(value)) {
                                            if (!column.getComments().contains("NTD") && !column.getComments().contains("??????")) {
                                                result.put("flag", "fail");
                                                result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "??????????????????????????????", "The contents of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " cannot be empty"));
                                                return result.getJson();
                                            }
                                        }
                                        if (column.getDataType().equalsIgnoreCase("number")) {
                                            try {
                                                if ("".equalsIgnoreCase(value.trim())) {
                                                    if (column.getComments().contains("NTD") || column.getComments().contains("??????")) {
                                                        value = "0";
                                                    }
                                                }
                                                Double.parseDouble(value);
                                            } catch (Exception e) {
                                                result.put("flag", "fail");
                                                result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "?????????????????????????????????" + value + "???", "The number format of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                return result.getJson();
                                            }
                                        }
                                        value = value.replaceAll("'", "''");

                                        if ("SBU".equalsIgnoreCase(column.getColumnName())) {
                                            sbu = value;
                                            sbuList.add(sbu);
                                            sbuSet.add(value);
                                            if (!subs.contains(sbu)) {
                                                result.put("flag", "fail");
                                                result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "????????????sbu???????????????" + value + "???", "The sbu of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                return result.getJson();
                                            }
                                            if ("FIT_PO_SBU_YEAR_CD_SUM".equalsIgnoreCase(tableName)) {
                                                if (sbuSet.size() > 1) {
                                                    result.put("flag", "fail");
                                                    result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "????????????sbu???????????????" + value + "???,sbu????????????", "The sbu of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                    return result.getJson();
                                                }
                                                if (!dataRangeStr.contains(sbu)) {
                                                    result.put("flag", "fail");
                                                    result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "????????????sbu???????????????" + value + "???,?????????????????????sbu?????????", "The sbu of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                    return result.getJson();
                                                }
                                            }
                                        }
                                        if ("COMMODITY_MAJOR".equalsIgnoreCase(column.getColumnName()) || "COMMODITY".equalsIgnoreCase(column.getColumnName())) {
                                            commodity = value;
                                            commodityList.add(value);
                                            commoditySet.add(value);
                                            if (!commoditys.contains(commodity)) {
                                                result.put("flag", "fail");
                                                result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "????????????commodity???????????????" + value + "???", "The commodity of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                return result.getJson();
                                            }
                                            if (commoditySet.size() > 1 && !"FIT_PO_SBU_YEAR_CD_SUM".equalsIgnoreCase(tableName)) {
                                                result.put("flag", "fail");
                                                result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "????????????commodity???????????????" + value + "???,????????????????????????", "The commodity of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                return result.getJson();
                                            }
                                            if (!"FIT_PO_SBU_YEAR_CD_SUM".equalsIgnoreCase(tableName)) {
                                                if (!dataRangeStr.contains(commodity)) {
                                                    result.put("flag", "fail");
                                                    result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "????????????commodity???????????????" + value + "???,?????????????????????commodity?????????", "The commodity of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                    return result.getJson();
                                                }
                                            }
                                        }
                                        if ("month".equalsIgnoreCase(column.getColumnName())) {
                                            if (value.length() < 2) {
                                                value = "0" + value;
                                            }
                                            monthList.add(value);
                                        }
                                        if ("PRICE_CONTROL".equalsIgnoreCase(column.getColumnName())) {

                                            if (!"?????????".equals(value) && !"??????".equals(value)) {
                                                result.put("flag", "fail");
                                                result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "???????????????????????????????????????" + value + "???", "The PRICE_CONTROL of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                return result.getJson();
                                            }
                                        }
                                        data.add(value);
                                    }
                                } else {
                                    if (column.getDataType().equalsIgnoreCase("date")) {
                                        try {
                                            Date date2 = ExcelUtil.getCellDateValue(row.getCell(n), DateUtil.SDF_ddSMMSyyyy);
                                            if (date2 != null) {
                                                data.add(DateUtil.formatByddSMMSyyyy(date2));
                                            } else {
                                                data.add("");
                                            }
                                        } catch (Exception e) {
                                            result.put("flag", "fail");
                                            result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "?????????????????????", "The format of date in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error"));
                                            return result.getJson();
                                        }
                                    } else {
                                        String value = ExcelUtil.getCellStringValue(row.getCell(n), i, j);
                                        if (StringUtils.isNotEmpty(value)) {
                                            if (column.getDataType().equalsIgnoreCase("number")) {
                                                try {
                                                    Double.parseDouble(value);
                                                } catch (Exception e) {
                                                    result.put("flag", "fail");
                                                    result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????" + (n + 1) + "?????????????????????????????????" + value + "???", "The number format of the cell in sheet " + (i + 1) + " row " + (j + 1) + " column " + (n + 1) + " is error)"));
                                                    return result.getJson();
                                                }
                                            }
                                            value = value.replaceAll("'", "''");
                                            data.add(value);
                                        } else {
                                            data.add("");
                                        }
                                    }
                                }
                            }
                            if (keySerialList.contains(column.getSerial())) {
                                String value = ExcelUtil.getCellStringValue(row.getCell(n), i, j);
                                key += value;
                            }
                            n++;
                        }
                        if (StringUtils.isNotEmpty(key)) {
                            String base64 = Base64.getEncoder().encodeToString(key.getBytes());
                            if (keyRepeatMap.containsKey(base64)) {
                                Integer rowIndex = keyRepeatMap.get(base64);
                                List<String> keyNameList = new ArrayList<String>();
                                for (PoKey poKey : keys) {
                                    if (poKey.getSerial() >= 5) {
                                        keyNameList.add(poKey.getComments());
                                    }
                                }
//                                result.put("flag", "fail");
//                                result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet???" + (j + 1) + "??????Key?????????" + rowIndex + "??????Key?????????,Key??????????????? : ", "The key value of the sheet " + (i + 1) + " row " + (j + 1) + " is repeated with the key value of row " + rowIndex + ". The key contains the following columns : ") + Arrays.toString(keyNameList.toArray()));
//                                return result.getJson();
                            } else {
                                keyRepeatMap.put(base64, j + 1);
                            }
                        }

                        dataList2.add(data);
                        Boolean isOne = oneByky(tableName, dataList2, data);
                        if (!isOne) {
                            dataList.add(data);
                        }
                    }
                    if (!dataList.isEmpty()) {
                        dataMap.put(poTable, dataList);
                    } else {
                        result.put("flag", "fail");
                        result.put("msg", getLanguage(locale, "???" + (i + 1) + "???sheet??????????????????", "The sheet " + (i + 1) + " has no valid data row"));
                    }
                }
                String taskId = poTableService.savePoData(dataMap, year, period, sbuList, commodityList, monthList);
                if ("".equals(taskId)) {
                    result.put("flag", "fail");
                    result.put("msg", "????????????????????????????????????????????????");
                } else if ("FIT_PO_CD_MONTH_DTL".equalsIgnoreCase(tableName)) {
                    try {
                        poTableService.validateMonth(taskId);
                    }catch (RuntimeException e){
                        result.put("flag", "fail");
                        result.put("msg", e.getMessage() + "?????????CD????????????,?????????????????????");
                    }catch (Exception e){
                        e.printStackTrace();
                        result.put("flag", "fail");
                        result.put("msg", "??????????????????????????????????????????");
                    }
                }
            } else {
                result.put("flag", "fail");
                result.put("msg", getLanguage(locale, "???????????????????????????????????????", "Uploaded file not received"));
            }
        } catch (Exception e) {
            logger.error("??????????????????", e);
            result.put("flag", "fail");
            result.put("msg", ExceptionUtil.getRootCauseMessage(e));
        }
        return result.getJson();
    }

    @RequestMapping(value = "download")
    @ResponseBody
    @Log(name = "????????????-->??????")
    public synchronized String download(HttpServletRequest request, HttpServletResponse response, PageRequest pageRequest, AjaxResult result,
                                        String DateYear,
                                        String date, String dateEnd, String tableNames,
                                        String poCenter, String sbuVal, String priceControl,String commodity,String founderVal,String buVal) {
        try {
            Locale locale = (Locale) WebUtils.getSessionAttribute(request, SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);

            XSSFWorkbook workBook = new XSSFWorkbook();
            XSSFCellStyle titleStyle = workBook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setFillForegroundColor(IndexedColors.BLACK.index);
            XSSFCellStyle lockStyle = workBook.createCellStyle();
            lockStyle.setAlignment(HorizontalAlignment.CENTER);
            lockStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(217, 217, 217)));
            lockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFCellStyle unlockStyle = workBook.createCellStyle();
            unlockStyle.setAlignment(HorizontalAlignment.CENTER);
            XSSFFont font = workBook.createFont();
            font.setColor(IndexedColors.WHITE.index);
            font.setBold(true);
            titleStyle.setFont(font);
            String[] tables = tableNames.split(",");
            SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook(workBook);
            for (String tableName : tables) {
                if ("FIT_PO_CD_MONTH_DTL".equalsIgnoreCase(tableName)) {
                    tableName = "FIT_PO_CD_MONTH_DOWN";
                }
                PoTable poTable = poTableService.get(tableName);
                List<PoColumns> columns = poTable.getColumns();
                List<Integer> lockSerialList = new ArrayList<Integer>();
                String sql = "select ";
                Sheet sheet = sxssfWorkbook.createSheet(getByLocale(locale, poTable.getComments()));
                sheet.createFreezePane(0, 1, 0, 1);
                Row titleRow = sheet.createRow(0);
                List<Integer> numberList = new ArrayList<Integer>();
                for (int i = 0; i < columns.size(); i++) {
                    PoColumns poColumn = columns.get(i);
                    String columnName = poColumn.getColumnName();
                    String comments = poColumn.getComments();
                    comments = getByLocale(locale, comments);
                    if (poColumn.getLocked()) {
                        lockSerialList.add(poColumn.getSerial());
                    }
                    if (poColumn.getDataType().equalsIgnoreCase("number")) {
                        //??????????????????
                        sql += columnName + ",";
                        numberList.add(i);
                    } else if (poColumn.getDataType().equalsIgnoreCase("date")) {
                        sql += "to_char(" + columnName + ",'dd/mm/yyyy'),";
                    } else {
                        sql += columnName + ",";
                    }

                    Cell cell = titleRow.createCell(i);
                    cell.setCellValue(comments);
                    cell.setCellStyle(titleStyle);
                    sheet.setColumnWidth(i, comments.getBytes("GBK").length * 256 + 400);
                }

                String whereSql = " where 1=1 ";
                if (StringUtils.isNotEmpty(date) && StringUtils.isNotEmpty(dateEnd)) {
                    Date d = DateUtil.parseByYyyy_MM(date);
                    Assert.notNull(d, getLanguage(locale, "??????????????????", "The format of year/month is error"));
                    String[] split = date.split("-");
                    String[] split1 = dateEnd.split("-");
                    String year = split[0];
                    String period = split[1];
                    String period1 = split1[1];
                    if (period.length() < 2) {
                        period = "0" + period;
                    }
                    if (period1.length() < 2) {
                        period1 = "0" + period1;
                    }
                    whereSql += " and " + columns.get(0).getColumnName() + " =" + year + " and " + columns.get(1).getColumnName() + ">=" + period + " and " + columns.get(1).getColumnName() + "<=" + period1;
                }
                if (StringUtils.isNotEmpty(poCenter)) {
                    whereSql += " and " + columns.get(2).getColumnName() + "='" + poCenter + "'";
                }


                switch (poTable.getTableName()){
                    //??????CD???????????????????????????
                    case "FIT_PO_BUDGET_CD_DTL":
                        if (StringUtils.isNotEmpty(priceControl)) {
                            whereSql += " and PRICE_CONTROL ='"+priceControl+"' ";
                        }
                        break;
                    //?????????????????????CD????????????????????????????????????
                    case "FIT_ACTUAL_PO_NPRICECD_DTL":
                        if (StringUtils.isNotEmpty(founderVal)) {
                            whereSql += " and TASK_ID in( select ID from FIT_PO_TASK where CREATE_USER ='" + founderVal + "') ";
                        }
                        break;
                    //??????CD??????by????????????
                    case "FIT_PO_CD_MONTH_DOWN":
                        whereSql = " where 1=1";
                        whereSql += " and year= " + DateYear;
                        if (StringUtils.isNotEmpty(priceControl)) {
                            whereSql += " and  PRICE_CONTROL='" + priceControl + "'";
                        }
                        break;
                    //??????CD??????by????????????
                    case "FIT_PO_Target_CPO_CD_DTL":
                        whereSql = " where 1=1";
                        whereSql += " and year= " + DateYear;
                        break;
                    //SBU??????CD???????????????
                    case "FIT_PO_SBU_YEAR_CD_SUM":
                        whereSql = " where 1=1";
                        if (StringUtils.isNotEmpty(DateYear)) {
                            whereSql += " and " + columns.get(0).getColumnName() + "='" + DateYear + "'";
                        }
                        if (StringUtils.isNotEmpty(poCenter)) {
                            whereSql += " and " + columns.get(1).getColumnName() + "='" + poCenter + "'";
                        }
                        if (StringUtils.isNotEmpty(priceControl)) {
                            whereSql += " and  PRICE_CONTROL='" + priceControl + "'";
                        }
                        break;
                }

                if(null!=commodity && !"".equalsIgnoreCase(commodity)){
                    String commotityVal="";
                    for (int i=0;i<commodity.split(",").length;i++) {
                        commotityVal+="'"+commodity.split(",")[i]+"',";
                    }
                    if ("FIT_PO_SBU_YEAR_CD_SUM".equalsIgnoreCase(poTable.getTableName())||
                            "FIT_PO_Target_CPO_CD_DTL".equalsIgnoreCase(poTable.getTableName())||
                            "FIT_PO_CD_MONTH_DOWN".equalsIgnoreCase(poTable.getTableName())) {
                        whereSql += " and COMMODITY_MAJOR in(" + commotityVal.substring(0,commotityVal.length()-1) + ")";
                    }else{
                        whereSql += " and COMMODITY in (" + commotityVal.substring(0,commotityVal.length()-1) + ")";
                    }
                }

                if (StringUtils.isNotEmpty(sbuVal)) {
                    whereSql += " and sbu LIKE " + "'%" + sbuVal + "%'";
                }
                if (StringUtils.isNotEmpty(buVal)) {
                    whereSql += " and bu LIKE " + "'%" + buVal + "%'";
                }
                sql = sql.substring(0, sql.length() - 1) + " from " + tableName + whereSql;
                //????????????????????????
                List<PoKey> keys = poTable.getKeys();
                String orderBy ="";
                if (keys != null && keys.size() > 0) {
                    for(int i=0;i<keys.size();i++){
                        orderBy = orderBy + keys.get(i).getColumnName() + ",";
                    }
                    sql=sql+" order by "+orderBy.substring(0,orderBy.length()-1);
                }
                System.out.println("??????******???"+sql);
                pageRequest.setPageSize(ExcelUtil.PAGE_SIZE);
                pageRequest.setPageNo(1);
                List<Object[]> dataList = poTableService.findPageBySql(pageRequest, sql).getResult();
                if (CollectionUtils.isNotEmpty(dataList)) {
                    int rowIndex = 1;
                    for (Object[] objects : dataList) {
                        Row contentRow = sheet.createRow(rowIndex++);
                        String generateType = objects[0].toString();
                        for (int i = 0; i < objects.length; i++) {
                            Cell cell = contentRow.createCell(i);
                            String text = (objects[i] != null ? objects[i].toString() : "");
                            if (StringUtils.isNotEmpty(text) && numberList.contains(i)) {
                                cell.setCellValue(Double.parseDouble(text));
                            } else {
                                cell.setCellValue(text);
                            }
                            if (i < 5 || EnumGenerateType.A.getCode().equals(generateType) || (EnumGenerateType.AM.getCode().equals(generateType) && lockSerialList.contains(new Integer(i)))) {
                                cell.setCellStyle(lockStyle);
                            } else {
                                cell.setCellStyle(unlockStyle);
                            }
                        }
                    }

                    while (dataList != null && dataList.size() >= ExcelUtil.PAGE_SIZE) {
                        pageRequest.setPageNo(pageRequest.getPageNo() + 1);
                        dataList = poTableService.findPageBySql(pageRequest, sql).getResult();
                        if (CollectionUtils.isNotEmpty(dataList)) {
                            for (Object[] objects : dataList) {
                                Row contentRow = sheet.createRow(rowIndex++);
                                String generateType = objects[0].toString();
                                for (int i = 0; i < objects.length-1; i++) {
                                    Cell cell = contentRow.createCell(i);
                                    String text = (objects[i] != null ? objects[i].toString() : "");
                                    if (StringUtils.isNotEmpty(text) && numberList.contains(i)) {
                                        cell.setCellValue(Double.parseDouble(text));
                                    } else {
                                        cell.setCellValue(text);
                                    }
                                    if (i < 5 || EnumGenerateType.A.getCode().equals(generateType) || (EnumGenerateType.AM.getCode().equals(generateType) && lockSerialList.contains(new Integer(i)))) {
                                        cell.setCellStyle(lockStyle);
                                    } else {
                                        cell.setCellStyle(unlockStyle);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            String fileName = tableNames;
            String tableNameSql = "select * from fit_po_table";
            List<PoTable> list = poTableService.listBySql(tableNameSql, PoTable.class);
            for (int i = 0; i < list.size(); i++) {
                if (fileName.equalsIgnoreCase(list.get(i).getTableName())) {
                    fileName = list.get(i).getComments().split("_")[1];
                    break;
                }
            }

            File outFile = new File(request.getRealPath("") + File.separator + "static" + File.separator + "download" + File.separator + fileName + ".xlsx");
            OutputStream out = new FileOutputStream(outFile);
            sxssfWorkbook.write(out);
            sxssfWorkbook.close();
            out.flush();
            out.close();
            result.put("fileName", outFile.getName());
            System.gc();
        } catch (Exception e) {
            logger.error("??????Excel??????", e);
            result.put("flag", "fail");
            result.put("msg", ExceptionUtil.getRootCauseMessage(e));
        }

        return result.getJson();
    }


    @RequestMapping(value = "template")
    @ResponseBody
    public synchronized String template(HttpServletRequest request, HttpServletResponse response, PageRequest pageRequest, AjaxResult result, String tableNames, String table_type) {
        Locale locale = (Locale) WebUtils.getSessionAttribute(request, SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
        try {
            XSSFWorkbook workBook = new XSSFWorkbook();
            XSSFCellStyle titleStyle = workBook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setFillForegroundColor(IndexedColors.BLACK.index);
            titleStyle.setBorderBottom(BorderStyle.THIN);
            titleStyle.setBorderColor(XSSFCellBorder.BorderSide.BOTTOM, new XSSFColor(new java.awt.Color(255, 255, 255)));

            XSSFCellStyle lockStyle = workBook.createCellStyle();
            lockStyle.setLocked(true);
            lockStyle.setAlignment(HorizontalAlignment.CENTER);
            lockStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(217, 217, 217)));
            lockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFCellStyle unlockStyle = workBook.createCellStyle();
            unlockStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFFont font = workBook.createFont();
            font.setColor(IndexedColors.WHITE.index);
            font.setBold(true);
            titleStyle.setFont(font);
            //	???????????????????????????
            XSSFCellStyle unlockstyle = workBook.createCellStyle();
            unlockstyle.setLocked(false);

            String fileName = "";
            for (String tableName : tableNames.split(",")) {
                fileName = tableName;
                PoTable poTable = poTableService.get(tableName);
                List<PoColumns> columns = poTable.getColumns();
                XSSFSheet sheet = workBook.createSheet(getByLocale(locale, poTable.getComments()));
                sheet.setDefaultColumnStyle(0, unlockStyle);
                sheet.setDefaultColumnStyle(1, unlockStyle);
                sheet.setDefaultColumnStyle(2, unlockStyle);
                sheet.setDefaultColumnStyle(3, unlockStyle);
                sheet.setDefaultColumnStyle(4, unlockStyle);

                sheet.createFreezePane(0, 2, 0, 2);
                int rowIndex = 0;
                Row row = sheet.createRow(rowIndex++);
                Row row1 = sheet.createRow(rowIndex++);
                for (int i = 0; i < columns.size(); i++) {
                    String comments = columns.get(i).getComments();
                    String examples = columns.get(i).getExamples();

                    comments = getByLocale(locale, comments);
                    Cell cell = row.createCell(i);
                    Cell cell1 = row1.createCell(i);

                    cell.setCellValue(comments);
                    cell1.setCellValue(examples);

                    cell.setCellStyle(titleStyle);
                    cell1.setCellStyle(titleStyle);
                    sheet.setColumnWidth(i, comments.getBytes("GBK").length * 300 + 300);
                    if (null != examples && comments.getBytes("GBK").length < examples.getBytes("GBK").length) {
                        sheet.setColumnWidth(i, examples.getBytes("GBK").length * 300 + 300);
                    }
                }
                sheet.enableLocking();
                CTSheetProtection sheetProtection = sheet.getCTWorksheet().getSheetProtection();
                sheetProtection.setSelectLockedCells(false);
                sheetProtection.setSelectUnlockedCells(false);
                sheetProtection.setFormatCells(true);
                sheetProtection.setFormatColumns(true);
                sheetProtection.setFormatRows(true);
                sheetProtection.setInsertColumns(true);
                sheetProtection.setInsertRows(false);
                sheetProtection.setInsertHyperlinks(true);
                sheetProtection.setDeleteColumns(true);
                sheetProtection.setDeleteRows(true);
                sheetProtection.setSort(false);
                sheetProtection.setAutoFilter(false);
                sheetProtection.setPivotTables(true);
                sheetProtection.setObjects(true);
                sheetProtection.setScenarios(true);
                //????????????????????????
                for (short i = 0; i < columns.size(); i++) {
                    sheet.setDefaultColumnStyle(i, unlockstyle);
                }
                //?????????????????????
                if ("FIT_ACTUAL_PO_NPRICECD_DTL".equalsIgnoreCase(tableName)) {
                    sheet.setDefaultColumnStyle(2, lockStyle);
                    sheet.setDefaultColumnStyle(4, lockStyle);
                    sheet.setDefaultColumnStyle(20, lockStyle);
                    sheet.setDefaultColumnStyle(26, lockStyle);
                    sheet.setDefaultColumnStyle(27, lockStyle);
                    sheet.setDefaultColumnStyle(28, lockStyle);
//                    sheet.setDefaultColumnStyle(28, lockStyle);
                } else if ("FIT_PO_SBU_YEAR_CD_SUM".equalsIgnoreCase(tableName)) {
                    sheet.setDefaultColumnStyle(1, lockStyle);
                    sheet.setDefaultColumnStyle(6, lockStyle);
                } else if ("FIT_PO_BUDGET_CD_DTL".equalsIgnoreCase(tableName)) {
                    sheet.setDefaultColumnStyle(2, lockStyle);
                    sheet.setDefaultColumnStyle(4, lockStyle);
                }
            }
            table_type = "PO";
            if (table_type.equals("PO")) {
                //List<String> commodityList = poTableService.listBySql("SELECT distinct tie.commodity_major FROM bidev.t_itmecategory_ebs tie order by tie.commodity_major");
                List<String> commodityList = poTableService.listBySql("select distinct tie.COMMODITY_NAME from CUX_FUNCTION_COMMODITY_MAPPING tie order by tie.COMMODITY_NAME");
                //SBU ?????????????????????
//                List<String> sbuList = poTableService.listBySql("select   distinct  SBU from EPMEBS.CUX_SBU_BU_MAPPING order by SBU ");

                List<String> sbuList = poTableService.listBySql(" select distinct tie.NEW_SBU_NAME from bidev.v_if_sbu_mapping tie where tie.NEW_SBU_NAME in('IDS','EMS','ABS','ACE','ASD','AEC','TSC','APS','CW','FAD','IoT','CIDA','Tengyang','TMTS','FIAD') order by tie.NEW_SBU_NAME ");

                Sheet sheet = workBook.createSheet("Commodity?????????SBU");
                Row titleRow = sheet.createRow(0);
                Cell cell = titleRow.createCell(0);
                Cell cellt = titleRow.createCell(1);
                cell.setCellStyle(titleStyle);
                cellt.setCellStyle(titleStyle);
                cell.setCellValue("Commodity Major");
                cellt.setCellValue("SBU");
                int n = 1;
                if (sbuList.size() >= commodityList.size()) {
                    for (int i = 0; i < sbuList.size(); i++) {
                        String commodity = "";
                        String sbu = sbuList.get(i);
                        if (i < commodityList.size()) {
                            commodity = commodityList.get(i);
                        }
                        Row row = sheet.createRow(n);
                        Cell cell1 = row.createCell(0);
                        Cell cell2 = row.createCell(1);
                        cell1.setCellValue(commodity);
                        cell2.setCellValue(sbu);
                        n++;
                    }
                } else {
                    for (int i = 0; i < commodityList.size(); i++) {
                        String sbu = "";
                        if (n <= sbuList.size()) {
                            sbu = sbuList.get(i);
                        }
                        String commodity = commodityList.get(i);
                        Row row = sheet.createRow(n);
                        Cell cell1 = row.createCell(0);
                        Cell cell2 = row.createCell(1);
                        cell1.setCellValue(commodity);
                        cell2.setCellValue(sbu);
                        n++;
                    }
                }


            }
            //??????????????????
            String tableNameSql = "select * from fit_po_table";
            List<PoTable> list = poTableService.listBySql(tableNameSql, PoTable.class);
            for (int i = 0; i < list.size(); i++) {
                if (fileName.equalsIgnoreCase(list.get(i).getTableName())) {
                    fileName = list.get(i).getComments().split("_")[1];
                    break;
                }
            }

            File outFile = new File(request.getRealPath("") + File.separator + "static" + File.separator + "download/" + fileName + ".xlsx");
            OutputStream out = new FileOutputStream(outFile);
            workBook.write(out);
            workBook.close();
            out.flush();
            out.close();

            result.put("fileName", outFile.getName());
        } catch (Exception e) {
            logger.error("????????????????????????", e);
            result.put("flag", "fail");
            result.put("msg", getLanguage(locale, "????????????????????????", "Fail to download template file") + " : " + ExceptionUtil.getRootCauseMessage(e));
        }

        return result.getJson();
    }

    /**
      ?????????????????????
      tableName ???????????????key
      dataList ???????????????
      data ????????????
     */
    public Boolean oneByky(String tableName, List<List<String>> dataList, List<String> data) {
        if (dataList.size() == 1) {
            return false;
        }
        int limit = 4;
        if ("FIT_PO_SBU_YEAR_CD_SUM".equalsIgnoreCase(tableName)) {
            limit = 5;
        } else if ("FIT_ACTUAL_PO_NPRICECD_DTL".equalsIgnoreCase(tableName)) {
            limit = 6;
        } else if ("FIT_PO_BUDGET_CD_DTL".equalsIgnoreCase(tableName)) {
            limit = 7;
        } else if ("FIT_PO_CD_MONTH_DTL".equalsIgnoreCase(tableName)) {
            limit = 4;
        }
        int num = 0;
        for (int i = dataList.size() - 1; i >= 0; i--) {
            List<String> perData = dataList.get(i);
            String breString = "";
            String afterString = "";
            for (int i1 = 0; i1 < limit; i1++) {
                breString += data.get(i1);
                afterString += perData.get(i1);
            }
            if (breString.equals(afterString)) {
                num += 1;
            }
        }
        if (num > 1) {
            return true;
        }
        return false;

    }

    @RequestMapping(value = "/selectCommdity")
    @ResponseBody
    public List<String> selectCommdity(HttpServletRequest request, String functionName){
        if(functionName.isEmpty()){
            List<String> commodityList=poTableService.listBySql("select distinct COMMODITY_MAJOR from BIDEV.v_dm_d_commodity_major ");
            return  commodityList;
        }
        List<String> commodityList=poTableService.listBySql("select distinct COMMODITY_MAJOR from BIDEV.v_dm_d_commodity_major where FUNCTION_NAME='"+functionName+"'");
        return  commodityList;
    }
}

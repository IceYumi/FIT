<%@page import="foxconn.fit.entity.base.EnumGenerateType"%>
<%@page import="foxconn.fit.util.SecurityUtils"%>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ include file="/static/common/taglibs.jsp"%>
<%
    String entity=SecurityUtils.getEntity();
    request.setAttribute("entity", entity);

//    ArrayList<String> poCenters = new ArrayList<String>(Arrays.asList(SecurityUtils.getPoCenter()));
//    request.setAttribute("poCenters",poCenters);

%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
    <meta http-equiv="pragma" content="no-cache">
    <meta http-equiv="cache-control" content="no-cache">
    <meta http-equiv="expires" content="0">
    <meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
    <meta http-equiv="description" content="This is my page">
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8" />
    <style type="text/css">
        .search-btn{
            height:40px;
            margin-left:10px;
            color:#ffffff;
            /*background-image: linear-gradient(to bottom, #fbb450, #f89406);*/
            /*background-color: #f89406 !important;*/
        }
        /*.ui-datepicker select.ui-datepicker-month{*/
        /*    display: none;*/
        /*}*/
        /*.ui-datepicker-calendar,.ui-datepicker-current{*/
        /*    display:none;*/
        /*}*/
        /*.ui-datepicker-close{float:none !important;}*/
        .ui-datepicker-buttonpane{text-align: center;}
        .table thead th{vertical-align: middle;}
        .table-condensed td{padding:5px 3px;}
    </style>
    <script type="text/javascript">
        $(function() {
            $("#task").show();
            $("#audit").hide();
            $("#Query").click(function () {
                $("#typeTip").hide();
                var type=$("#type").val();
                var taskStatus=$("#taskStatus").val();
                var name=$("#name").val();
                var QDate=$("#QDate").val();
                var QDateEnd=$("#QDateEnd").val();
                $("#loading").show();
                $("#Content").load("${ctx}/bi/poTaskList/list",{pageSize:15,taskStatus:taskStatus,type:type,
                    name:name,QDate:QDate,QDateEnd:QDateEnd},function(){$("#loading").fadeOut(1000);});
            }).click();


            $("#backBtna").click(function () {
                debugger
                $("#audit").hide();
                $("#taskStatus").show();
                $("#type").show();
                $("#name").show();
                $("#Query").show();
                $("#Query").click();
            })
            $("#QDate,#QDateEnd").datepicker({
                changeMonth: true,
                changeYear: true,
                dateFormat: 'yy-mm-dd',
                showButtonPanel: false,
                closeText: "<spring:message code='confirm'/>"
            });
        });

    </script>
</head>
<body>
<div class="row-fluid bg-white content-body">
    <div class="span12">
        <div class="page-header bg-white">
            <h2>
                <span><c:if test="${languageS eq 'zh_CN'}">??????????????????</c:if><c:if test="${languageS eq 'en_US'}">Sign off task query</c:if></span>
            </h2>
        </div>

        <div class="m-l-md m-t-md m-r-md" style="clear:both;">
            <div class="controls" id="task">
                <ul style="float:left;">
                    <li>
                        <select id="type" class="input-large"  style="width:200px;margin-bottom:0;">
                            <option value="">?????????????????????</option>
                            <option value="FIT_PO_BUDGET_CD_DTL">??????CD????????????</option>
                            <option value="FIT_PO_SBU_YEAR_CD_SUM">SBU??????CD????????????</option>
                            <option value="FIT_PO_Target_CPO_CD_DTL">??????CD ??????CPO?????????</option>
                            <option value="FIT_ACTUAL_PO_NPRICECD_DTL">?????????????????????CD??????</option>
                            <option value="FIT_PO_CD_MONTH_DOWN">??????CDby????????????</option>
                        </select>
                    </li>
                    <li style="height:30px;">
                        <span id="typeTip" style="display:none;" class="Validform_checktip Validform_wrong"><spring:message code='please_select'/></span>
                    </li>
                </ul>
                <ul style="float:left;margin-left:20px;">
                    <li>
                        <select id="taskStatus" class="input-large"  style="width:200px;margin-bottom:0;">
                            <option value="">
                                <c:if test="${languageS eq 'zh_CN'}">?????????????????????</c:if>
                                <c:if test="${languageS eq 'en_US'}">Select task status</c:if>
                                </option>
                            <option value="0"><c:if test="${languageS eq 'zh_CN'}">?????????</c:if>
                                <c:if test="${languageS eq 'en_US'}">Unsubmitted</c:if></td></option>
                            <option value="1"><spring:message code='praeiudicium'/></option>
                            <option value="2">
                                <c:if test="${languageS eq 'zh_CN'}">??????</c:if>
                                <c:if test="${languageS eq 'en_US'}">Final Judgment</c:if>
                                </option>
                            <option value="3">
                                <c:if test="${languageS eq 'zh_CN'}">??????</c:if>
                                <c:if test="${languageS eq 'en_US'}">Finish</c:if>
                            </option>
                            <option value="-1">
                                <c:if test="${languageS eq 'zh_CN'}">??????</c:if>
                                <c:if test="${languageS eq 'en_US'}">Turn Down</c:if>
                            </option>
                        </select>
                    </li>
                </ul>
                <ul style="float:left;margin-left:20px;">
                    <li>
                        <input id="name" style="float:left;width:140px;text-align:center;margin-bottom:0;" placeholder="<c:if test="${languageS eq 'zh_CN'}">?????????????????????</c:if><c:if test="${languageS eq 'en_US'}">Please enter a task name</c:if>" type="text">
                    </li>
                </ul>
                <ul style="float:left;margin-left:20px;" >
                    <li>
                        <input id="QDate" style="float:left;width:140px;text-align:center;margin-bottom:0;"
                               placeholder="<spring:message code='start_time'/>"
                               type="text" value="" readonly>
                    </li>
                </ul>
                <ul style="float:left;">
                    <li>
                        <input id="QDateEnd" style="float:left;width:140px;text-align:center;margin-bottom:0;"
                               type="text" value=""
                               placeholder="<spring:message code='end_time'/>"
                               readonly>
                    </li>
                </ul>
                <button id="Query" class="btn search-btn btn-warning m-l-md" style="margin-left:20px;float:left;" type="submit"><spring:message code='query'/></button>
            </div>
            <div style="height:55px;!important;" class="controls" id="audit">
                <button  id="backBtna" class="btn search-btn btn-primary" style="background-image: linear-gradient(to bottom, #aad83e, #aad83e);background-color: #aad83e;" type="submit">
                    <c:if test="${languageS eq 'zh_CN'}">??????</c:if>
                    <c:if test="${languageS eq 'en_US'}">return</c:if>
                    </button>
            </div>
        </div>
        <div class="p-l-md p-r-md p-b-md" id="Content"></div>
    </div>
</div>
</body>
</html>

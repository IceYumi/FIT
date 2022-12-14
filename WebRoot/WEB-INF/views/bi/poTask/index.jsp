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
        .ui-datepicker select.ui-datepicker-month{
            display: none;
        }
        .ui-datepicker-calendar,.ui-datepicker-current{
            display:none;
        }
        .ui-datepicker-close{float:none !important;}
        .ui-datepicker-buttonpane{text-align: center;}
        .table thead th{vertical-align: middle;}
        .table-condensed td{padding:5px 3px;}
    </style>
    <script type="text/javascript">
        $(function() {
            $("#task").show();
            $("#audit").hide();
            $("#ui-datepicker-div").remove();
            $("#Date,#QDate,#DateEnd").datepicker({
                changeMonth: true,
                changeYear: true,
                dateFormat: 'yy-MM',
                showButtonPanel:true,
                closeText:"<spring:message code='confirm'/>"
            });

            $("#Date,#QDate,#DateEnd").click(function(){
                periodId=$(this).attr("id");
                $(this).val("");
            });

            $("#ui-datepicker-div").on("click", ".ui-datepicker-close", function() {
                var year = $("#ui-datepicker-div .ui-datepicker-year option:selected").val();//????????????????????????
                $("#"+periodId).val(year);//???input??????????????????????????????1?????????????????????
                if($("#"+periodId+"Tip").length>0){
                    $("#"+periodId+"Tip").hide();
                }
            });
            $("#Query").click(function () {
                $("#typeTip").hide();
                var type=$("#type").val();
                var name=$("#name").val();
                var date=$("#QDate").val();
                var roleCode=$("#roleCode").val();
                $("#loading").show();
                $("#Content").load("${ctx}/bi/poTask/list",{date:date,type:type,name:name,roleCode:roleCode},function(){$("#loading").fadeOut(1000);});

            }).click();

        });

        $('#backBtn').click(function () {
            $("#audit").hide();
            $("#task").show();
            $("#roleCode").show();
            $("#QDate").show();
            $("#type").show();
            $("#name").show();
            $("#Query").show();
            // var roleCode=$("#roleCode").val();
            $("#Query").click();
            <%--$("#Content").load("${ctx}/bi/poTask/list",{pageNo:"1",pageSize:"10",roleCode:roleCode},function(){$("#loading").fadeOut(1000);});--%>
        })

        var periodId;
    </script>
</head>
<body>
<div class="row-fluid bg-white content-body">
    <div class="span12">
        <div class="page-header bg-white">
            <h2>
                <span><c:if test="${languageS eq 'zh_CN'}">?????????</c:if><c:if test="${languageS eq 'en_US'}">Pending approval</c:if></span>
            </h2>
        </div>

        <div class="m-l-md m-t-md m-r-md" style="clear:both;">
            <div class="controls" id="task">
                <ul style="float:left;">
                    <li>
                        <select style="float:left;width:140px" id="roleCode" class="input-large"  style="width:200px;margin-bottom:0;">
                            <c:forEach items="${roles}" var="role">
                                <option value="${role.CODE}">${role.NAME}</option>
                            </c:forEach>
                        </select>
                    </li>
                    <li style="height:30px;">
                        <span id="roleTypeTip" style="display:none;" class="Validform_checktip Validform_wrong"><spring:message code='please_select'/></span>
                    </li>
                </ul>
                <ul style="float:left;margin-left:20px;">
                    <li>
                        <input id="QDate" style="float:left;width:140px;text-align:center;margin-bottom:0;" placeholder="<spring:message code='please_select'/>???" type="text" value="" readonly>
                    </li>
                </ul>
                <ul style="float:left;margin-left:20px;">
                    <li>
                        <select id="type" class="input-large"  style="width:200px;margin-bottom:0;">
                            <option value="">??????</option>
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
                        <input id="name" style="float:left;width:140px;text-align:center;margin-bottom:0;" placeholder="?????????????????????" type="text">
                    </li>
                </ul>
                <button id="Query" class="btn search-btn btn-warning m-l-md" style="margin-left:20px;float:left;" type="submit"><spring:message code='query'/></button>
            </div>
            <div style="height:55px;!important;" class="controls" id="audit">
                <button  id="backBtn" class="btn search-btn btn-primary" style="margin-left: -20px;background-image: linear-gradient(to bottom, #aad83e, #aad83e);background-color: #aad83e;" type="submit">??????</button>
            </div>
        </div>
        <div class="p-l-md p-r-md p-b-md" id="Content"></div>
        <div id="modal-audit" style="display:none;">
            <form id="taskForm" class="form-horizontal">
                <input id="id" style="display: none;"/>
                <div class="control-group" style="margin:0px 0px 20px 10px;">
                    <div class="pull-left">
                        <i class="icon-asterisk need m-r-sm" title="<spring:message code='required'/>"></i>
                        ?????????<input id="taskName2" style="height: 30px !important;" type="text" datatype="s3-30" readonly/>
                    </div>
                </div>
                <div class="control-group" style="margin:0px 0px 20px 10px;">
                    <div class="pull-left">
                        <i class="icon-asterisk need m-r-sm" title="<spring:message code='required'/>"></i>
                        ?????????<select id="flag" name="flag" style="width:100px" datatype="*" nullmsg="<spring:message code='please_select'/>">
                        <option value="0">??????</option>
                        <option value="1">??????</option>
                    </select>
                    </div>
                </div>
                <div class="control-group" style="margin:0px 0px 20px 10px;">
                    <div class="pull-left" style="margin-left: 40px">
                        ???????????????<input name="remark" style="height: 30px !important;" type="text" datatype="s3-30" nullmsg="<spring:message code='please_input'/>" errormsg="<spring:message code='s3_30'/>"/>
                    </div>
                </div>
            </form>
        </div>

        <div id="modal-audit1" style="display:none;">
            <form id="taskForm1" class="form-horizontal">
                <input id="id1" style="display: none;"/>
                <div class="control-group" style="margin:0px 0px 20px 10px;">
                    <div class="pull-left">
                        <i class="icon-asterisk need m-r-sm" title="<spring:message code='required'/>"></i>
                        ?????????<input id="taskName21" style="height: 30px !important;" type="text" datatype="s3-30" readonly/>
                    </div>
                </div>
                <div class="control-group" style="margin:0px 0px 20px 10px;">
                    <div class="pull-left">
                        ???????????????<input name="remark" style="height: 30px !important;" type="text" datatype="s3-30" nullmsg="<spring:message code='please_input'/>" errormsg="<spring:message code='s3_30'/>"/>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>

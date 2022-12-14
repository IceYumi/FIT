<%@page import="foxconn.fit.entity.base.EnumGenerateType" %>
<%@page import="foxconn.fit.util.SecurityUtils" %>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8" %>
<%@ include file="/static/common/taglibs.jsp" %>
<%
    String entity = SecurityUtils.getEntity();
    request.setAttribute("entity", entity);
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
    <meta http-equiv="pragma" content="no-cache">
    <meta http-equiv="cache-control" content="no-cache">
    <meta http-equiv="expires" content="0">
    <meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
    <meta http-equiv="description" content="This is my page">
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
    <style type="text/css">
        .search-btn {
            height: 40px;
            margin-left: 10px;
            color: #ffffff;
            background-image: linear-gradient(to bottom, #fbb450, #f89406);
            background-color: #f89406 !important;
        }

        .ui-datepicker-calendar, .ui-datepicker-current {
            display: none;
        }

        .ui-datepicker-close {
            float: none !important;
        }

        .ui-datepicker-buttonpane {
            text-align: center;
        }

        .table thead th {
            vertical-align: middle;
        }

        .table-condensed td {
            padding: 1px 5px !important;
        }
        .modal-backdrop {
            position: initial!important;
        }
    </style>
    <script type="text/javascript">
        $(function () {
            $("#ui-datepicker-div").remove();
            $("#Date,#QDate,#DateEnd,#QDateEnd").datepicker({
                changeMonth: true,
                changeYear: true,
                dateFormat: 'yy-MM',
                showButtonPanel: true,
                closeText: "<spring:message code='confirm'/>"
            });
            $("#QDate,#DateEnd,#QDateEnd").click(function () {
                periodId = $(this).attr("id");
                $(this).val("");
            });

            $("#ui-datepicker-div").on("click", ".ui-datepicker-close", function () {
                var month = $("#ui-datepicker-div .ui-datepicker-month option:selected").val();//????????????????????????
                var year = $("#ui-datepicker-div .ui-datepicker-year option:selected").val();//????????????????????????
                $("#" + periodId).val(year + '-' + (parseInt(month) + 1));//???input??????????????????????????????1?????????????????????
                if ($("#" + periodId + "Tip").length > 0) {
                    $("#" + periodId + "Tip").hide();
                }
            });

            $("input[type='radio']").change(function () {
                if($(this).val()=='FIT_PO_CD_MONTH_DTL'){
                    $("#cdFormula").show();
                }else{
                    $("#cdFormula").hide();
                }
            });

            $("#QTableName").change(function () {
                $("#Content table tr").remove();
                $("#Fenye").remove();
                $.ajax({
                    type: "POST",
                    url: "${ctx}/bi/poIntegrationList/downloadCheck",
                    async: true,
                    dataType: "json",
                    data: {
                        tableName: $("#QTableName").val()
                    },
                    success: function (data) {
                        if (data.flag == "success") {
                            $("#Download").show();
                        } else {
                            $("#Download").hide();
                        }
                    }
                });
                if ($(this).val().length > 0) {
                    $("#" + $(this).attr("id") + "Tip").hide();
                }
                $("#Query input").val("");
                $("#Query select").val("");
                $("#NTD").hide();
                $("#QpoCenter").show();
                $("#Scenario").text("");
                $("#buVal").show();
                $("#priceControl").show();
                $("#founderVal").hide();
                switch ($("#QTableName").val()) {
                    //?????????????????????CD?????????
                    case "FIT_ACTUAL_PO_NPRICECD_DTL":
                        $("input[name='YYYY']").hide();
                        $("input[name='YYYYMM']").show();
                        $("#priceControl").hide();
                        $("#founderVal").show();
                        $("#buVal").hide();
                        break;
                    //??????CD???????????????
                    case "FIT_PO_BUDGET_CD_DTL":
                        $("input[name='YYYY']").hide();
                        $("input[name='YYYYMM']").show();
                        $("#Scenario").text("Scenario:Actual")
                        break;
                    //SBU??????CD???????????????
                    case "FIT_PO_SBU_YEAR_CD_SUM":
                        $("input[name='YYYY']").show();
                        $("input[name='YYYYMM']").hide();
                        $("#Scenario").text("Scenario:Budget");
                        break;
                    //??????CD??????by????????????
                    case "FIT_PO_CD_MONTH_DTL":
                        $("input[name='YYYY']").show();
                        $("input[name='YYYYMM']").hide();
                        $("#QpoCenter").hide();
                        $("#QpoCenter").change();
                        $("#NTD").show();
                        break;
                }
            });

            $("#QueryBtn").click(function () {
                var tableName = $("#QTableName").val();
                if (tableName.length == 0) {
                    $("#QTableNameTip").show();
                    return;
                }
                var DateYear = $("#DateYear").val();
                var date = $("#QDate").val();
                var dateEnd = $("#QDateEnd").val();
                if(tableName=='FIT_PO_SBU_YEAR_CD_SUM'||tableName=='FIT_PO_CD_MONTH_DTL'){
                    var r = /^\+?[1-9][0-9]*$/;
                    if (DateYear.length!=4 || !r.test(DateYear)) {
                      layer.alert("????????????????????????(Please fill in the correct year)");
                        return;
                    }
                }else{
                    if (date.length == 0) {
                        layer.alert("????????????????????????(Please select a start date)");
                        return;
                    }
                    if (dateEnd.length == 0) {
                        layer.alert("????????????????????????(Please select an end date)");
                        return;
                    }
                    if(date.substr(0,3)!=dateEnd.substr(0,3)){
                        layer.alert("?????????????????????????????????????????????(Please select the date of the same year)");
                        return;
                    }
                }
                var entity = $("#QpoCenter").val();
                var sbuVal = $("#sbuVal").val();
                $("#QTableNameTip").hide();
                $("#QpoCenterTip").hide();
                $("#PageNo").val(1);
                $("#loading").show();
                $("#Content").load("${ctx}/bi/poIntegrationList/list", {
                    date: date,
                    dateEnd: dateEnd,
                    DateYear: DateYear,
                    tableName: tableName,
                    poCenter: entity,
                    sbuVal: sbuVal,
                    priceControl: $("#priceControl").val(),
                    commodity: $("#commodity").val(),
                    buVal: $("#buVal").val(),
                    founderVal: $("#founderVal").val()
                }, function () {
                    $("#loading").fadeOut(1000);
                });
            });

            $('#deleteBtn').click(function () {
                var ids = $('input[type=checkbox]');
                var data = '';
                ids.each(function () {
                    //?????????????????????????????????
                    if ($(this).prop("checked")) {
                        data = data + $(this).val() + ",";
                    }
                });
                if(data===""){
                    layer.alert("??????????????????????????????(Select the data to delete)");
                }else{
                    data = data.substring(0, data.length - 1);
                    console.log(data)
                    var tableName = $("#QTableName").val();
                    var obj={
                        id:data,
                        tableName: tableName
                    }
                    $.ajax({
                        type:"POST",
                        url:"${ctx}/bi/poIntegrationList/delete",
                        async:false,
                        dataType:"json",
                        data:obj,
                        success: function(data){
                            layer.alert(data.msg);
                            refresh();
                        },
                        error: function(XMLHttpRequest, textStatus, errorThrown) {
                            layer.alert("<spring:message code='connect_fail'/>");
                        }
                    });
                }
                //???????????????
            })

            //selectCommdity
            $("#QpoCenter").change(function (e) {
                $.ajax({
                    type:"POST",
                    url:"${ctx}/bi/poIntegrationList/selectCommdity",
                    async:false,
                    dataType:"json",
                    data:{
                        functionName:$(this).val()
                    },
                    success: function(data){
                        $("#commdityTable").empty();
                        var commdityTr=0;
                        jQuery.each(data, function (i, item) {
                            if (i % 4 == 0) {
                                $("#commdityTable").append("<tr id='commdityTr"+i+"'></tr>");
                                commdityTr=i;
                            }
                            $("#commdityTr"+commdityTr).append("<td height='25px' width='140px'> <input type='checkbox' class='userGroupVal' value='" + item + "'>" + item + "</td>");
                        })
                    },
                    error: function() {
                        layer.alert("<spring:message code='connect_fail'/>");
                    }
                });
            })
        });

        var periodId;
        $("#affirmBut").click(function () {
            var valueUser='';
            $(".userGroupVal:checked").each(function () {
                valueUser+=$(this).val()+",";
            })
            $("#commodity").val(valueUser.substring(0,valueUser.length-1));
        })
        $("#closeBut").click(function () {
            $(".userGroupVal:checked").prop("checked",false);
            $("#commodity").val();
        })
        $("#allCheck").click(function(){
            if ($("#allCheck").prop("checked") == true) {
                $(".userGroupVal").prop("checked", true);
            } else {
                $(".userGroupVal").prop("checked", false);
            }
        });


        $("#Download").click(function () {
            if ($("#QTableName").val().length == 0) {
                $("#QTableNameTip").show();
                return;
            }
            var tableName = $("#QTableName").val();
            var date = $("#QDate").val();
            var dateEnd = $("#QDateEnd").val();
            var DateYear = $("#DateYear").val();
            if(tableName=='FIT_PO_SBU_YEAR_CD_SUM'||tableName=='FIT_PO_CD_MONTH_DTL'){
                var r = /^\+?[1-9][0-9]*$/;
                if (DateYear.length!=4 || !r.test(DateYear)) {
                    layer.alert("????????????????????????(Please fill in the correct year)");
                    return;
                }
            }else{
                if (date.length == 0) {
                    layer.alert("????????????????????????(Please select a start date)");
                    return;
                }
                if (dateEnd.length == 0) {
                    layer.alert("????????????????????????(Please select an end date)");
                    return;
                }
                if(date.substr(0,3)!=dateEnd.substr(0,3)){
                    layer.alert("?????????????????????????????????????????????(Please select the date of the same year)");
                    return;
                }
            }
            var entity = $("#QpoCenter").val();
            var sbuVal = $("#sbuVal").val();
            $("#loading").show();
            $.ajax({
                type: "POST",
                url: "${ctx}/bi/poIntegration/download",
                async: true,
                dataType: "json",
                data: {date: date,
                    dateEnd: dateEnd,
                    DateYear: DateYear,
                    tableNames: tableName,
                    poCenter: entity,
                    sbuVal: sbuVal,
                    priceControl:$("#priceControl").val(),
                    commodity:$("#commodity").val(),
                    buVal: $("#buVal").val(),
                    founderVal: $("#founderVal").val()
                },
                success: function (data) {
                    $("#loading").hide();
                    if (data.flag == "success") {
                        window.location.href = "${ctx}/static/download/" + data.fileName;
                    } else {
                        layer.alert(data.msg);
                    }
                },
                error: function (XMLHttpRequest, textStatus, errorThrown) {
                    $("#loading").hide();
                    layer.alert("<spring:message code='connect_fail'/>");
                }
            });
        });

        $(document).ready(function(){
            if("${detailsTsak}"=="ok"){
                // setTimeout(function () {
                    $("#QTableName").val("FIT_PO_SBU_YEAR_CD_SUM");
                    $("#QTableName").change();
                    $("#DateYear").val("${DateYear}");
                    $("#QueryBtn").click();
                // },1000)
            }
        })
    </script>
</head>
<body>
<div class="row-fluid bg-white content-body">
    <div class="span12">
        <div class="page-header bg-white">
            <h2>
                <span><spring:message code='poIntegrationList'/></span>
            </h2>
        </div>
        <div class="m-l-md m-t-md m-r-md" style="clear:both;">
            <div style="margin-top: 20px;">
                <ul style="float:left;margin-right:10px;">
                    <li>
                        <select id="QTableName" class="input-large" style="width:200px;margin-bottom:0;">
                            <option value=""><spring:message code='tableSelect'/></option>
                            <c:forEach items="${tableListSelect }" var="poTable">
                                <option value="${poTable.tableName }">${poTable.comments }</option>
                            </c:forEach>
                        </select>
                    </li>
                    <li style="height:20px;">
                        <span id="QTableNameTip" style="display:none;"
                              class="Validform_checktip Validform_wrong"><spring:message code='please_select'/></span>
                    </li>
                </ul>
                <span id="Query">
                <span id="Scenario"></span>
                <input id="DateYear" name="YYYY" type="text"
                       style="width:80px;text-align:center;display: none;"
                       placeholder="<spring:message code='year'/>">
                <input id="QDate" name="YYYYMM"
                       style="width:80px;text-align:center;"
                       placeholder="<spring:message code='start_time'/>"
                       type="text" value="" readonly>
                <input id="QDateEnd" name="YYYYMM"
                       style="width:80px;text-align:center;"
                       type="text" value=""
                       placeholder="<spring:message code='end_time'/>"
                       readonly>
                <select id="QpoCenter" name="QpoCenter" class="input-large"
                        style="width:120px;">
                    <option value=""><spring:message code='poCenter'/></option>
                    <c:forEach items="${poCenters}" var="code">
                        <option value="${code}">${code}</option>
                    </c:forEach>
                </select>
                <input type="text" id="commodity" style="width: 120px;" data-toggle="modal"
                       data-target="#myModal" placeholder="commodity">
                <input type="text" style="width: 120px;" id="buVal" value="${buVal}"
                       placeholder="BU">
                <input type="text" style="width: 120px;" id="sbuVal" value="${sbuVal}"
                       placeholder="SBU">
                <input type="text" style="width: 120px;display: none;" id="founderVal" value="${founderVal}"
                       placeholder="<spring:message code='founder'/>">
                <select id="priceControl" name="priceControl" class="input-large"
                        style="width:100px;display: none;">
                    <option value="">????????????</option>
                    <option value="??????">??????</option>
                    <option value="?????????">?????????</option>
                </select>
                </span>
                <button id="QueryBtn" class="btn search-btn"
                        type="submit"><spring:message code='query'/></button>
                <c:if test="${hasKey eq '1'}">
                    <button id="deleteBtn" class="btn search-btn"
                            type="submit"><spring:message code='delete'/></button>
                </c:if>
                <button id="Download" class="btn search-btn" type="button">
                    <spring:message code='download'/></button>
            </div>
        </div>
        <div id="NTD" style="clear: both;margin-left: 20px;display: none;"><h5>?????????K NTD</h5></div>
        <div class="p-l-md p-r-md p-b-md" id="Content"></div>
    </div>
</div>

<div class="modal fade" id="myModal" style="display: none" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">
                    &times;
                </button>
                <h4 class="modal-title" id="myModalLabel">
                    commodity
                </h4>
                <span>?????? <input id="allCheck" type="checkbox"></span>
            </div>
            <div class="modal-body">
                <table id="commdityTable" border="0" cellpadding="0" cellspacing="1">
                    <c:forEach items="${commodityList}" var="column" varStatus="status">
                    <c:if test="${status.index %4 eq 0}">
                    <tr>
                        </c:if>
                        <td  height="25px" width="140px">
                            <input type="checkbox" class="userGroupVal" value="${column}">${column}
                        </td>
                        </c:forEach>
                </table>
            </div>
            <div class="modal-footer">
                <button type="button" id="closeBut" class="btn btn-default" data-dismiss="modal"><spring:message code="close"/>
                </button>
                <button type="button" id="affirmBut" class="btn btn-primary" data-dismiss="modal"><spring:message code="submit"/></button>
            </div>
        </div>
    </div>
</div>

</body>
</html>

package foxconn.fit.entity.base;

/**
 * 预算用户菜单
 * @author 陈亮
 */
public enum EnumBudgetMenu {
	productNoUnitCost("產品料號單位成本"),
	budgetProductNoUnitCost("預算產品料號單位成本"),
	budgetForecastDetailRevenue("預算營收明細"),
	predictDetailRevenue("銷貨收入預測表"),
	forecastDetailRevenue("營收明細");
	
	private EnumBudgetMenu(String name){
		this.name = name;
	}
	
	private final String name;

	public String getName() {
		return name;
	}
	
	public String getCode(){
		return this.name();
	}
	
}
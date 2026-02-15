package march.examples.spring_boot_project.services;

import org.springframework.stereotype.Service;
import march.annotations.MarchProvider;
import march.annotations.MarchTool;
import march.enums.ToolType;
import java.util.List;

@Service
@MarchProvider
public class FinanceService {

    @MarchTool(name = "calculate_compound_interest", description = "Calculates the compound interest. Formula: A = P(1 + r/n)^(nt). Returns the final amount.", type = ToolType.FUNCTION)
    public Double calculateCompoundInterest(Double principal, Double annualRate, Integer timesCompoundedPerYear, Double years) {
        return principal * Math.pow(1 + (annualRate / timesCompoundedPerYear), timesCompoundedPerYear * years);
    }

    @MarchTool(name = "calculate_simple_interest", description = "Calculates simple interest. Formula: I = P * r * t", type = ToolType.FUNCTION)
    public Double calculateSimpleInterest(Double principal, Double annualRate, Double years) {
        return principal * annualRate * years;
    }

    @MarchTool(name = "calculate_loan_monthly_payment", description = "Calculates the monthly payment for a loan (Amortization). Formula: M = P [ r(1+r)^n ] / [ (1+r)^n – 1 ]. Rate should be monthly (annual/12), n is total months.", type = ToolType.FUNCTION)
    public Double calculateLoanMonthlyPayment(Double principal, Double annualInterestRate, Integer termsInMonths) {
        Double monthlyRate = annualInterestRate / 12;
        return (principal * monthlyRate * Math.pow(1 + monthlyRate, termsInMonths)) / (Math.pow(1 + monthlyRate, termsInMonths) - 1);
    }

    @MarchTool(name = "calculate_roi", description = "Calculates Return on Investment (ROI). Formula: ((Net Profit / Cost of Investment) * 100)", type = ToolType.FUNCTION)
    public Double calculateROI(Double currentValue, Double costOfInvestment) {
        return ((currentValue - costOfInvestment) / costOfInvestment) * 100;
    }

    @MarchTool(name = "calculate_break_even_point", description = "Calculates Break-Even Point in units. Formula: Fixed Costs / (Sales Price per Unit - Variable Cost per Unit)", type = ToolType.FUNCTION)
    public Double calculateBreakEvenPoint(Double fixedCosts, Double salesPricePerUnit, Double variableCostPerUnit) {
        return fixedCosts / (salesPricePerUnit - variableCostPerUnit);
    }

    @MarchTool(name = "calculate_net_present_value", description = "Calculates NPV for a series of cash flows. Rate is the discount rate.", type = ToolType.FUNCTION)
    public Double calculateNetPresentValue(Double discountRate, Double initialInvestment, List<Double> cashFlows) {
        double npv = -initialInvestment;
        for (int i = 0; i < cashFlows.size(); i++) {
            npv += cashFlows.get(i) / Math.pow(1 + discountRate, i + 1);
        }
        return npv;
    }

    @MarchTool(name = "calculate_future_value", description = "Calculates Future Value (FV). Formula: FV = PV * (1 + r)^n", type = ToolType.FUNCTION)
    public Double calculateFutureValue(Double presentValue, Double periodicRate, Double numberOfPeriods) {
        return presentValue * Math.pow(1 + periodicRate, numberOfPeriods);
    }

    @MarchTool(name = "calculate_future_value_annuity", description = "Calculates FV of an Ordinary Annuity. Formula: FV = P * [((1 + r)^n - 1) / r]", type = ToolType.FUNCTION)
    public Double calculateFutureValueAnnuity(Double paymentPerPeriod, Double periodicRate, Double numberOfPeriods) {
        return paymentPerPeriod * ((Math.pow(1 + periodicRate, numberOfPeriods) - 1) / periodicRate);
    }

    @MarchTool(name = "calculate_rule_of_72", description = "Estimates years to double an investment using Rule of 72. Formula: 72 / Annual Rate of Return", type = ToolType.FUNCTION)
    public Double calculateRuleOf72(Double annualRateOfReturnPercent) {
        return 72 / annualRateOfReturnPercent;
    }

    @MarchTool(name = "calculate_cagr", description = "Calculates Compound Annual Growth Rate (CAGR). Formula: (Ending Value / Beginning Value)^(1/n) - 1", type = ToolType.FUNCTION)
    public Double calculateCAGR(Double beginningValue, Double endingValue, Double numberOfYears) {
        return Math.pow(endingValue / beginningValue, 1.0 / numberOfYears) - 1;
    }

    @MarchTool(name = "calculate_debt_to_income_ratio", description = "Calculates Debt-to-Income (DTI) ratio. Formula: (Total Monthly Debt Payments / Gross Monthly Income) * 100", type = ToolType.FUNCTION)
    public Double calculateDTI(Double totalMonthlyDebt, Double grossMonthlyIncome) {
        return (totalMonthlyDebt / grossMonthlyIncome) * 100;
    }

    @MarchTool(name = "calculate_inflation_impact", description = "Calculates the future cost of an item given inflation. Formula: Present Cost * (1 + Inflation Rate)^Years", type = ToolType.FUNCTION)
    public Double calculateInflationImpact(Double presentCost, Double inflationRate, Double years) {
        return presentCost * Math.pow(1 + inflationRate, years);
    }

    @MarchTool(name = "calculate_markup_percentage", description = "Calculates Markup Percentage. Formula: ((Sales Price - Unit Cost) / Unit Cost) * 100", type = ToolType.FUNCTION)
    public Double calculateMarkupPercentage(Double unitCost, Double salesPrice) {
        return ((salesPrice - unitCost) / unitCost) * 100;
    }

    @MarchTool(name = "calculate_margin_percentage", description = "Calculates Margin Percentage. Formula: ((Sales Price - Unit Cost) / Sales Price) * 100", type = ToolType.FUNCTION)
    public Double calculateMarginPercentage(Double unitCost, Double salesPrice) {
        return ((salesPrice - unitCost) / salesPrice) * 100;
    }

     @MarchTool(name = "convert_currency_mock", description = "Converts currency using fixed rates (Mock implementation). Supported: USD, EUR, GBP, JPY.", type = ToolType.FUNCTION)
    public Double convertCurrencyMock(Double amount, String fromCurrency, String toCurrency) {
        // Simple base rates against USD
        Double eurRate = 1.1; // 1 EUR = 1.1 USD
        Double gbpRate = 1.3; // 1 GBP = 1.3 USD
        Double jpyRate = 0.007; // 1 JPY = 0.007 USD
        
        Double amountInUsd = 0.0;
        switch (fromCurrency.toUpperCase()) {
            case "USD": amountInUsd = amount; break;
            case "EUR": amountInUsd = amount * eurRate; break;
            case "GBP": amountInUsd = amount * gbpRate; break;
            case "JPY": amountInUsd = amount * jpyRate; break;
            default: throw new IllegalArgumentException("Unsupported currency: " + fromCurrency);
        }

        switch (toCurrency.toUpperCase()) {
            case "USD": return amountInUsd;
            case "EUR": return amountInUsd / eurRate;
            case "GBP": return amountInUsd / gbpRate;
            case "JPY": return amountInUsd / jpyRate;
            default: throw new IllegalArgumentException("Unsupported currency: " + toCurrency);
        }
    }
}

package march.examples.spring_boot_project.services;

import org.springframework.stereotype.Service;
import march.annotations.MarchProvider;
import march.annotations.MarchTool;
import march.enums.ToolType;

@Service
@MarchProvider
public class DebtService {

    @MarchTool(name = "calculate_interest", description = "Calculates simple interest given principal, rate and time", type = ToolType.FUNCTION)
    public Double calculateInterest(Double principal, Double rate, Double time) {
        return principal * rate * time;
    }

    @MarchTool(name = "calculate_total_debt", description = "Calculates total debt by adding principal and interest", type = ToolType.FUNCTION)
    public Double calculateTotalDebt(Double principal, Double interest) {
        return principal + interest;
    }
}

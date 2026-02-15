package march.agent;

public class CostMetrics {
    private double perDay;
    private double perWeek;
    private double perMonth;
    private double perYear;

    public CostMetrics(double perDay, double perWeek, double perMonth, double perYear) {
        this.perDay = perDay;
        this.perWeek = perWeek;
        this.perMonth = perMonth;
        this.perYear = perYear;
    }

    public double getPerDay() { return perDay; }
    public double getPerWeek() { return perWeek; }
    public double getPerMonth() { return perMonth; }
    public double getPerYear() { return perYear; }

    @Override
    public String toString() {
        return String.format("Day: $%.2f, Week: $%.2f, Month: $%.2f, Year: $%.2f", perDay, perWeek, perMonth, perYear);
    }
}

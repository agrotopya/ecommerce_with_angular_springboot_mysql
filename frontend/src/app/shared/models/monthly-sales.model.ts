// src/app/shared/models/monthly-sales.model.ts
export interface MonthlySalesDto {
  monthYear: string; // Format: "YYYY-MM" veya "MonthName YYYY"
  totalSales: number; // Backend'de Double, frontend'de number
}

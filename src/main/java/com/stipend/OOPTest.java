
package com.stipend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class OOPTest {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== STIPEND DATABASE OOP TEST ===");
        System.out.print("Enter your registered username: ");

        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            scanner.close();
            return;
        }

        String sql = """
                SELECT student_id, name, course, username
                FROM students
                WHERE username = ?
                """;

        try (Connection connection = DatabaseManager.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    System.out.println(
                            "Account not found. Check your username."
                    );
                    return;
                }

                Student student = new Student(
                        result.getInt("student_id"),
                        result.getString("name"),
                        result.getString("course"),
                        result.getString("username")
                );

                FinancialSystem system =
                        new FinancialSystem(student);

                System.out.println("\nAccount found!");
                student.displayStudentInfo();

                List<Transaction> transactions =
                        system.getTransactions();

                System.out.println("\n=== SAVED TRANSACTIONS ===");

                if (transactions.isEmpty()) {
                    System.out.println("No transactions found.");
                } else {
                    for (Transaction transaction : transactions) {
                        System.out.println(
                                transaction.displayTxn()
                        );
                    }
                }

                System.out.println("\n=== FINANCIAL SUMMARY ===");
                System.out.printf(
                        "Total Income: ₱%.2f%n",
                        system.getTotalIncome()
                );
                System.out.printf(
                        "Total Expenses: ₱%.2f%n",
                        system.getTotalExpenses()
                );
                System.out.printf(
                        "Remaining Balance: ₱%.2f%n",
                        system.getBalance()
                );

                System.out.println(
                        "\nDatabase OOP test completed successfully!"
                );
            }

        } catch (SQLException ex) {
            System.out.println(
                    "Database test failed: " + ex.getMessage()
            );
            ex.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}

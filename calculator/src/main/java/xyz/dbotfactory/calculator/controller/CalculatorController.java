package xyz.dbotfactory.calculator.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.dbotfactory.calculator.model.*;
import xyz.dbotfactory.calculator.service.CalculatorService;

import java.util.List;

@RestController
@RequestMapping("/calc")
public class CalculatorController {

    private final CalculatorService calculatorService;

    @Autowired
    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @GetMapping("/total_balance")
    public List<BalanceChange> getTotalBalance(@RequestBody List<BalanceChange> request){
        return calculatorService.calculateTotalBalance(request);
    }

    @GetMapping("/return_strategy")
    public List<DebtReturnTransaction> getDebtReturnStrategy(@RequestBody List<BalanceChange> request){
        return calculatorService.getDebtReturnStrategy(request);
    }
}

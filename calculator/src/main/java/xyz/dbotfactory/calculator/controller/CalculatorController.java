package xyz.dbotfactory.calculator.controller;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.dbotfactory.calculator.model.*;
import xyz.dbotfactory.calculator.service.CalculatorService;

import java.util.List;

@RestController
@RequestMapping("/calc")
@Log
public class CalculatorController {

    private final CalculatorService calculatorService;

    @Autowired
    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/total_balance")
    public List<BalanceChange> getTotalBalance(@RequestBody List<BalanceChange> request){
        log.info("new total balance request:" + request);
        return calculatorService.calculateTotalBalance(request);
    }

    @PostMapping("/return_strategy")
    public List<DebtReturnTransaction> getDebtReturnStrategy(@RequestBody List<BalanceChange> request){
        log.info("new return_strategy request:" + request);
        return calculatorService.getDebtReturnStrategy(request);
    }
}

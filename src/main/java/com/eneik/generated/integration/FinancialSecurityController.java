package com.eneik.generated.integration;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financial")
public class FinancialSecurityController {

    private final FinancialAccessControlService accessControlService;
    private final BudgetDocumentRepository budgetDocumentRepository;

    public FinancialSecurityController(FinancialAccessControlService accessControlService,
                                       BudgetDocumentRepository budgetDocumentRepository) {
        this.accessControlService = accessControlService;
        this.budgetDocumentRepository = budgetDocumentRepository;
    }

    @GetMapping("/documents")
    public List<BudgetDocument> getDocuments(@RequestHeader(value = "X-Moodle-Role", required = false) String roleHeader) {
        accessControlService.enforcePermission(roleHeader, "READ");
        return budgetDocumentRepository.findAll();
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetDocument createDocument(@RequestHeader(value = "X-Moodle-Role", required = false) String roleHeader,
                                         @RequestBody BudgetDocument document) {
        accessControlService.enforcePermission(roleHeader, "WRITE");
        return budgetDocumentRepository.save(document);
    }
}

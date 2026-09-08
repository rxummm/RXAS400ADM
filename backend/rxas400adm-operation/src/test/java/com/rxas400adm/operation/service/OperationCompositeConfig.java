package com.rxas400adm.operation.service;

import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.RiskLevel;
import com.rxas400adm.operation.domain.StepResult;
import com.rxas400adm.operation.executor.OperationExecutor;
import com.rxas400adm.operation.mapper.DesiredStateMapper;
import com.rxas400adm.operation.mapper.OperationMapper;
import com.rxas400adm.operation.mapper.OperationStepMapper;
import com.rxas400adm.operation.policy.ConfirmationPolicy;
import com.rxas400adm.operation.policy.OperationPolicyEngine;
import com.rxas400adm.operation.policy.RiskPolicy;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.security.DangerousClCommandValidator;
import com.rxas400adm.operation.policy.CommandPolicy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mockito.Mockito;

@TestConfiguration
@Import({OperationPolicyEngine.class, RiskPolicy.class, ConfirmationPolicy.class})
public class OperationCompositeConfig {

    @Bean
    public OperationMapper stubOperationMapper() {
        OperationMapper mockMapper = Mockito.mock(OperationMapper.class);
        Mockito.when(mockMapper.selectPageOrderByRequestedAt(Mockito.any(Page.class)))
               .thenAnswer(invocation -> invocation.getArgument(0));
        return mockMapper;
    }

    @Bean
    public OperationStepMapper stubOperationStepMapper() {
        OperationStepMapper mockMapper = Mockito.mock(OperationStepMapper.class);
        Mockito.when(mockMapper.selectListByOperationId(Mockito.anyLong()))
               .thenReturn(List.of());
        return mockMapper;
    }

    @Bean
    public DesiredStateMapper stubDesiredStateMapper() {
        return Mockito.mock(DesiredStateMapper.class);
    }

    @Bean
    public AS400ClientProvider stubAS400ClientProvider() {
        return Mockito.mock(AS400ClientProvider.class);
    }

    @Bean
    public DangerousClCommandValidator stubDangerousClCommandValidator() {
        return Mockito.mock(DangerousClCommandValidator.class);
    }

    @Bean
    public CommandPolicy stubCommandPolicy() {
        return Mockito.mock(CommandPolicy.class);
    }

    @Bean
    public OperationExecutor stubCreateExecutor() {
        return new StubCreateExecutor();
    }

    @Bean
    public OperationExecutor stubEndJobExecutor() {
        return new StubEndJobExecutor();
    }

    @Bean
    public OperationExecutor stubRawClExecutor() {
        return new StubRawClExecutor();
    }

    @IbmiOperation(code = "USER_CREATE", riskLevel = RiskLevel.WRITE, requiredPermission = "USER_CREATE")
    public static class StubCreateExecutor implements OperationExecutor {

        @Override
        public List<String> defineSteps() {
            return List.of("STUB_STEP");
        }

        @Override
        public StepResult executeStep(Operation op, String stepCode) {
            return StepResult.success("OK", "stub", 0);
        }
    }

    @IbmiOperation(code = "END_JOB", riskLevel = RiskLevel.DESTRUCTIVE, requiredPermission = "JOB_END")
    public static class StubEndJobExecutor implements OperationExecutor {

        @Override
        public List<String> defineSteps() {
            return List.of("STUB_END_STEP");
        }

        @Override
        public StepResult executeStep(Operation op, String stepCode) {
            return StepResult.success("OK", "stub", 0);
        }
    }

    @IbmiOperation(code = "RAW_CL", riskLevel = RiskLevel.BREAK_GLASS, requiredPermission = "BREAK_GLASS_EXECUTE")
    public static class StubRawClExecutor implements OperationExecutor {

        @Override
        public List<String> defineSteps() {
            return List.of("STUB_CL_STEP");
        }

        @Override
        public StepResult executeStep(Operation op, String stepCode) {
            return StepResult.success("OK", "stub", 0);
        }
    }


}

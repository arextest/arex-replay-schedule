package com.arextest.schedule.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.arextest.schedule.service.ConfigProvider;
import com.arextest.schedule.service.DefaultConfigProviderImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ApplicationConfigurationTest {
    @Mock
    private List<ConfigProvider> configProviders;
    @InjectMocks
    private ApplicationConfiguration applicationConfiguration;
    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    void configProvider() {
        ConfigProvider configProvider = applicationConfiguration.configProvider();
        assertEquals(DefaultConfigProviderImpl.class, configProvider.getClass());
    }
}

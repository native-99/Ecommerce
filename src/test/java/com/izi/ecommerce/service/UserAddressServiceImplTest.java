package com.izi.ecommerce.service;

import com.izi.ecommerce.common.errors.ForbiddenAccessException;
import com.izi.ecommerce.entity.UserAddress;
import com.izi.ecommerce.model.UserAddressRequest;
import com.izi.ecommerce.model.UserAddressResponse;
import com.izi.ecommerce.repository.UserAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceImplTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private UserAddressRepository userAddressRepository;

    @InjectMocks
    private UserAddressServiceImpl userAddressService;

    private UserAddress address;

    @BeforeEach
    void setUp() {
        address = UserAddress.builder()
                .userAddressId(10L)
                .userId(OWNER_ID)
                .addressName("Home")
                .streetAddress("Owner Street")
                .city("Jakarta")
                .isDefault(false)
                .build();
    }

    @Test
    void findById_returnsAddressForOwner() {
        when(userAddressRepository.findById(10L)).thenReturn(Optional.of(address));

        UserAddressResponse response = userAddressService.findById(OWNER_ID, 10L);

        assertEquals("Owner Street", response.getStreetAddress());
    }

    @Test
    void findById_rejectsOtherUser() {
        when(userAddressRepository.findById(10L)).thenReturn(Optional.of(address));

        assertThrows(ForbiddenAccessException.class,
                () -> userAddressService.findById(OTHER_USER_ID, 10L));
    }

    @Test
    void update_rejectsOtherUser() {
        when(userAddressRepository.findById(10L)).thenReturn(Optional.of(address));

        assertThrows(ForbiddenAccessException.class,
                () -> userAddressService.update(OTHER_USER_ID, 10L, new UserAddressRequest()));

        verify(userAddressRepository, never()).save(any());
    }

    @Test
    void delete_rejectsOtherUser() {
        when(userAddressRepository.findById(10L)).thenReturn(Optional.of(address));

        assertThrows(ForbiddenAccessException.class,
                () -> userAddressService.delete(OTHER_USER_ID, 10L));

        verify(userAddressRepository, never()).delete(any());
    }
}

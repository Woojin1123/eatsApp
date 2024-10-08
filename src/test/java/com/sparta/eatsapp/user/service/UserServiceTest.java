package com.sparta.eatsapp.user.service;

import static com.sparta.eatsapp.TestData.testAuth;
import static com.sparta.eatsapp.TestData.testPassword;
import static com.sparta.eatsapp.TestData.testUser;
import static com.sparta.eatsapp.TestData.userPatchRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.sparta.eatsapp.address.repository.AddressRepository;
import com.sparta.eatsapp.common.exception.AuthException;
import com.sparta.eatsapp.common.exception.NotFoundException;
import com.sparta.eatsapp.user.dto.request.UserPatchRequest;
import com.sparta.eatsapp.user.dto.response.UserResponse;
import com.sparta.eatsapp.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private AddressRepository addressRepository;
  @InjectMocks
  private UserService userService;

  @BeforeEach
  public void setUp() {
    Long userid = 1L;
    ReflectionTestUtils.setField(testUser, "id", userid);
    ReflectionTestUtils.setField(testUser, "isDeleted", false);
    ReflectionTestUtils.setField(testUser, "password", testPassword);
  }

  @Test
  public void 유저_정보_조회_성공() {
    //given
    Long userid = 1L;
    given(userRepository.findById(anyLong())).willReturn(Optional.ofNullable(testUser));
    //when
    UserResponse userResponse = userService.getUser(userid);
    //then
    assertNotNull(userResponse);
    assertEquals(userResponse.getEmail(), testUser.getEmail());
  }

  @Test
  public void 유저_조회_시_유저가_없을_경우_에러() {
    //given
    Long userId = 1L;
    given(userRepository.findById(anyLong())).willReturn(Optional.empty());
    //when & then
    NotFoundException exception = assertThrows(NotFoundException.class,
        () -> userService.getUser(userId));
    assertEquals("등록된 유저가 없습니다.", exception.getMessage());
  }

  @Test
  public void 유저_정보_수정_성공() {
    //given
    Long userId = 1L;

    given(userRepository.findById(anyLong())).willReturn(Optional.ofNullable(testUser));
    given(userRepository.save(any())).willReturn(testUser);
    //when
    UserResponse userResponse = userService.updateUser(testAuth, userId, userPatchRequest);
    //then
    assertNotNull(userResponse);
    assertEquals(userPatchRequest.getNickname(), userResponse.getNickname());
  }

  @Test
  public void 유저_정보_수정_시_권한이_없을_경우_에러() {
    //given
    Long userId = 2L;
    given(userRepository.findById(anyLong())).willReturn(Optional.ofNullable(testUser));
    ReflectionTestUtils.setField(testUser, "id", userId);
    //when & then
    AuthException exception = assertThrows(AuthException.class,
        () -> userService.updateUser(testAuth, userId, userPatchRequest)
    );
    assertEquals("권한이 없습니다.", exception.getMessage());
  }

  @Test
  public void 위치_주소를_하나만_입력_시_에러() {
    //given
    Long userId = 1L;
    UserPatchRequest testPatchRequest = UserPatchRequest.builder()
        .nickname("땅콩")
        .address("제주도")
        .build();
    given(userRepository.findById(anyLong())).willReturn(Optional.ofNullable(testUser));
    //when & then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> userService.updateUser(testAuth, userId, testPatchRequest)
    );
    assertEquals("위치와 주소는 둘 다 입력해야 합니다.", exception.getMessage());
  }

  @Test
  public void 유저_삭제_성공() {
    //given
    Long userId = 1L;
    given(userRepository.findById(anyLong())).willReturn(Optional.of(testUser));
    given(userRepository.save(any())).willReturn(testUser);
    //when
    Long deletedUserId = userService.deleteUser(userId, testAuth);
    //then
    assertTrue(testUser.getDeleted());
  }

  @Test
  public void 유저가_삭제_된_경우_에러() {
    //given
    Long userid = 1L;
    given(userRepository.findById(anyLong())).willReturn(Optional.ofNullable(testUser));
    ReflectionTestUtils.setField(testUser, "isDeleted", true);
    //when & then
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> userService.getUser(userid)
    );
    assertEquals("삭제된 유저입니다.", exception.getMessage());


  }

}

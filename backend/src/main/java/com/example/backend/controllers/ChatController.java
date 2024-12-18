package com.example.backend.controllers;

import com.example.backend.Exceptions.GlobalExceptionHandler;
import com.example.backend.dtos.ApiResp;
import com.example.backend.dtos.DtoMapper;
import com.example.backend.dtos.chat.ChatGroupDto;
import com.example.backend.models.PembeliModel;
import com.example.backend.models.PenjualModel;
import com.example.backend.models.chat.ChatGroup;
import com.example.backend.models.chat.ChatMessage;
import com.example.backend.dtos.chat.MessageCreateRequest;
import com.example.backend.services.ChatService;
import com.example.backend.services.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@CrossOrigin
@RestController
@RequestMapping("/chat")
@AllArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;

    private final JwtService jwtService ;

    private final DtoMapper mapper ;


    @PostMapping("/create-group")
    public ResponseEntity<ApiResp<ChatGroupDto>> createChatGroup(@RequestParam UUID penjualId) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the authenticated user is a Pembeli
        if (!(authentication.getPrincipal() instanceof PembeliModel)) {
            throw new GlobalExceptionHandler.UnauthorizedAccessException("Only Pembeli can initiate a new chat group");
        }

        PembeliModel pembeli = (PembeliModel) authentication.getPrincipal();
        ChatGroup chatGroup = chatService.createChatGroup(penjualId, pembeli.getIdPembeli());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResp<>(HttpStatus.CREATED.value(),  "Success Create chat group" , mapper.toChatGroupDto(chatGroup)));
    }

    @GetMapping("/messages/{chatGroupId}")
    public ResponseEntity<List<ChatMessage>> getChatMessages(@PathVariable UUID chatGroupId) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is part of this chat group
        if (authentication.getPrincipal() instanceof PenjualModel) {
            PenjualModel penjual = (PenjualModel) authentication.getPrincipal();
            chatService.validatePenjualAccessToChatGroup(penjual.getIdPenjual(), chatGroupId);
        } else if (authentication.getPrincipal() instanceof PembeliModel) {
            PembeliModel pembeli = (PembeliModel) authentication.getPrincipal();
            chatService.validatePembeliAccessToChatGroup(pembeli.getIdPembeli(), chatGroupId);
        } else {
            throw new GlobalExceptionHandler.UnauthorizedAccessException("Invalid user type");
        }

        List<ChatMessage> messages = chatService.getChatMessages(chatGroupId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResp<List<ChatGroupDto>>> getUserChatGroups() {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Determine user type and ID based on authentication
        UUID userId;
        String userType;

        if (authentication.getPrincipal() instanceof PenjualModel) {
            PenjualModel penjual = (PenjualModel) authentication.getPrincipal();
            userId = penjual.getIdPenjual();
            userType = "PENJUAL";
        } else if (authentication.getPrincipal() instanceof PembeliModel) {
            PembeliModel pembeli = (PembeliModel) authentication.getPrincipal();
            userId = pembeli.getIdPembeli();
            userType = "PEMBELI";
        } else {
            throw new GlobalExceptionHandler.UnauthorizedAccessException("Invalid user type");
        }

        List<ChatGroup> chatGroups = chatService.getUserChatGroups(userId, userType);
        return ResponseEntity.ok(
                new ApiResp<>(HttpStatus.OK.value(),
                        "Success retrieve chat group" ,
                        chatGroups.stream().map(mapper :: toChatGroupDto).collect(Collectors.toList())
                )
        );
    }

    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<ApiResp<Object>> deleteGroupChat (@PathVariable UUID groupId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof PembeliModel pembeliModel) {
            chatService.deleteByGroupId(groupId , pembeliModel);
            return ResponseEntity.ok(
                    new ApiResp<>(HttpStatus.OK.value(),
                            "Success delete chat group with id " + groupId ,
                            null)
            );
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body( new ApiResp<>(HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                null));
    }

    @MessageMapping("/chat/{chatGroupId}/send-message")
    @SendTo("/topic/chat/{chatGroupId}")
    public ChatMessage sendChatMessage(
            @DestinationVariable UUID chatGroupId,
            @Payload MessageCreateRequest messageCreateRequest
    ) {
        UUID senderId;
        String senderType;

        Object authentication = jwtService.validateWebSocketToken(messageCreateRequest.getToken());
        if (authentication instanceof PenjualModel penjual) {
            senderId = penjual.getIdPenjual();
            senderType = "PENJUAL";
        } else if (authentication instanceof PembeliModel pembeli) {
            senderId = pembeli.getIdPembeli();
            senderType = "PEMBELI";
        } else {
            throw new GlobalExceptionHandler.UnauthorizedAccessException("Invalid user type");
        }

        UUID recipientId = chatService.getRecipientId(chatGroupId, senderId);

        return chatService.sendMessage(chatGroupId, senderId, senderType, recipientId, messageCreateRequest.getContent());
    }

}

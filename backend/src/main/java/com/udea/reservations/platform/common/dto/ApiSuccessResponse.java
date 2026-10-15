package com.udea.reservations.platform.common.dto;

public record ApiSuccessResponse<T>(String message, T data) {}
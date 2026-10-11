export interface UserResponse {
  id: number;
  name: string;
  email: string;
  role: string;
  createdAt: string;
}

export interface UpdateUserRequest {
  name?: string;
  email?: string;
}

export interface MessageResponse {
  message: string;
}

export interface ApiError {
  status?: number;
  message: string;
  timestamp?: string;
  errors?: Record<string, string>;
}

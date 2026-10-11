export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  isAdmin?: boolean;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface DecodedToken {
  sub?: string;
  userId?: number;
  role?: string;
  name?: string;
  type?: string;
  jti?: string;
  iat?: number;
  exp?: number;
  [key: string]: any;
}

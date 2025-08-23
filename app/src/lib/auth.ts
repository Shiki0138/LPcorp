import { NextAuthOptions } from 'next-auth';
import { PrismaAdapter } from '@next-auth/prisma-adapter';
import CredentialsProvider from 'next-auth/providers/credentials';
import bcrypt from 'bcryptjs';
import { prisma } from './database';
import type { User } from '@/types';

declare module 'next-auth' {
  interface Session {
    user: {
      id: string;
      email: string;
      name?: string;
      role: 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN';
      status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
    };
  }
  
  interface User {
    id: string;
    email: string;
    name?: string;
    role: 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN';
    status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    id: string;
    role: 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN';
    status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
  }
}

export const authOptions: NextAuthOptions = {
  adapter: PrismaAdapter(prisma),
  providers: [
    CredentialsProvider({
      name: 'credentials',
      credentials: {
        email: { label: 'Email', type: 'email' },
        password: { label: 'Password', type: 'password' },
      },
      async authorize(credentials) {
        if (!credentials?.email || !credentials?.password) {
          throw new Error('Email and password are required');
        }

        // Find user in database
        const user = await prisma.user.findUnique({
          where: { email: credentials.email },
          include: {
            clientProfile: true,
          },
        });

        if (!user) {
          throw new Error('Invalid email or password');
        }

        if (user.status === 'SUSPENDED') {
          throw new Error('Account suspended. Please contact support.');
        }

        if (user.status === 'INACTIVE') {
          throw new Error('Account deactivated. Please contact support.');
        }

        // For demo purposes, we'll create a simple password check
        // In production, you should store hashed passwords
        const isPasswordValid = await bcrypt.compare(
          credentials.password,
          user.password || '' // Assume password field exists in schema
        );

        if (!isPasswordValid) {
          throw new Error('Invalid email or password');
        }

        return {
          id: user.id,
          email: user.email,
          name: user.name,
          role: user.role,
          status: user.status,
        };
      },
    }),
  ],
  session: {
    strategy: 'jwt',
    maxAge: 30 * 24 * 60 * 60, // 30 days
  },
  jwt: {
    maxAge: 30 * 24 * 60 * 60, // 30 days
  },
  pages: {
    signIn: '/auth/signin',
    signOut: '/auth/signout',
    error: '/auth/error',
  },
  callbacks: {
    async jwt({ token, user, trigger, session }) {
      if (user) {
        token.id = user.id;
        token.role = user.role;
        token.status = user.status;
      }
      
      // Handle session update
      if (trigger === 'update' && session) {
        token.name = session.name;
        token.email = session.email;
      }

      return token;
    },
    async session({ session, token }) {
      if (token) {
        session.user.id = token.id;
        session.user.role = token.role;
        session.user.status = token.status;
      }
      return session;
    },
    async signIn({ user, account, profile }) {
      // Allow sign in only for active users
      return user.status === 'ACTIVE' || user.status === 'PENDING_VERIFICATION';
    },
  },
  events: {
    async signIn({ user, account, profile, isNewUser }) {
      // Log successful sign in
      console.log(`User ${user.email} signed in`);
      
      // Update last login
      await prisma.user.update({
        where: { id: user.id },
        data: { updatedAt: new Date() },
      });
    },
    async signOut({ token, session }) {
      // Log sign out
      console.log(`User ${token?.email || session?.user?.email} signed out`);
    },
  },
  debug: process.env.NODE_ENV === 'development',
};

/**
 * Hash password using bcrypt
 */
export async function hashPassword(password: string): Promise<string> {
  const saltRounds = parseInt(process.env.BCRYPT_ROUNDS || '12', 10);
  return bcrypt.hash(password, saltRounds);
}

/**
 * Verify password against hash
 */
export async function verifyPassword(password: string, hashedPassword: string): Promise<boolean> {
  return bcrypt.compare(password, hashedPassword);
}

/**
 * Create new user with hashed password
 */
export async function createUser(userData: {
  email: string;
  password: string;
  name?: string;
  role?: 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN';
}): Promise<User> {
  const { email, password, name, role = 'CLIENT' } = userData;
  
  // Check if user already exists
  const existingUser = await prisma.user.findUnique({
    where: { email },
  });
  
  if (existingUser) {
    throw new Error('User with this email already exists');
  }
  
  // Hash password
  const hashedPassword = await hashPassword(password);
  
  // Create user
  const user = await prisma.user.create({
    data: {
      email,
      password: hashedPassword,
      name,
      role,
      status: 'ACTIVE',
    },
    include: {
      clientProfile: true,
      projects: true,
    },
  });
  
  return user;
}

/**
 * Role-based access control middleware
 */
export function requireRole(allowedRoles: ('CLIENT' | 'ADMIN' | 'SUPER_ADMIN')[]) {
  return (userRole: 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN') => {
    return allowedRoles.includes(userRole);
  };
}

/**
 * Check if user has permission for resource
 */
export async function hasPermission(
  userId: string,
  resource: string,
  action: string
): Promise<boolean> {
  const user = await prisma.user.findUnique({
    where: { id: userId },
  });
  
  if (!user || user.status !== 'ACTIVE') {
    return false;
  }
  
  // Super admin has all permissions
  if (user.role === 'SUPER_ADMIN') {
    return true;
  }
  
  // Admin permissions
  if (user.role === 'ADMIN') {
    const adminPermissions = [
      'project:read',
      'project:create',
      'project:update',
      'project:delete',
      'client:read',
      'client:update',
      'file:read',
      'file:create',
      'file:update',
      'file:delete',
    ];
    return adminPermissions.includes(`${resource}:${action}`);
  }
  
  // Client permissions - can only access their own resources
  if (user.role === 'CLIENT') {
    const clientPermissions = [
      'project:read',
      'project:create',
      'file:read',
      'file:create',
      'profile:read',
      'profile:update',
    ];
    return clientPermissions.includes(`${resource}:${action}`);
  }
  
  return false;
}

/**
 * Get user by ID with full profile
 */
export async function getUserById(userId: string): Promise<User | null> {
  return prisma.user.findUnique({
    where: { id: userId },
    include: {
      clientProfile: true,
      projects: {
        include: {
          files: true,
        },
      },
    },
  });
}

/**
 * Update user profile
 */
export async function updateUserProfile(
  userId: string,
  profileData: Partial<{
    name: string;
    email: string;
    phone: string;
    companyName: string;
    website: string;
    address: string;
  }>
): Promise<User> {
  const { name, email, ...clientProfileData } = profileData;
  
  return prisma.user.update({
    where: { id: userId },
    data: {
      name,
      email,
      clientProfile: {
        upsert: {
          create: clientProfileData,
          update: clientProfileData,
        },
      },
    },
    include: {
      clientProfile: true,
      projects: true,
    },
  });
}
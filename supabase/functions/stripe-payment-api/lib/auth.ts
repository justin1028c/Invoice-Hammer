import { createRemoteJWKSet, jwtVerify } from "jose";

export type AuthenticatedUser = { uid: string };

const googleKeys = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

export async function requireFirebaseUser(req: Request): Promise<AuthenticatedUser> {
  const projectId = Deno.env.get("FIREBASE_PROJECT_ID")?.trim();
  if (!projectId) throw new AuthError("FIREBASE_PROJECT_ID is not configured.", 500);

  const header = req.headers.get("authorization")?.trim() ?? "";
  const match = /^Bearer\s+(.+)$/i.exec(header);
  if (!match) throw new AuthError("Authentication required.", 401);

  try {
    const { payload } = await jwtVerify(match[1], googleKeys, {
      issuer: `https://securetoken.google.com/${projectId}`,
      audience: projectId,
      algorithms: ["RS256"],
    });
    if (!payload.sub) throw new Error("Token subject is missing.");
    return { uid: payload.sub };
  } catch {
    throw new AuthError("Invalid or expired authentication token.", 401);
  }
}

export class AuthError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

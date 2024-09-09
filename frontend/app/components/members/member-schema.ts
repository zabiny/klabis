import { z } from 'zod'
export const memberSchema = z.object({
  firstName: z.string(),
  lastName: z.string(),
  registrationNumber: z.string(),
  // trainingGroup: z.string(),
})

export type Member = z.infer<typeof memberSchema>

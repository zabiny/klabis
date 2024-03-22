import type { MiddlewareHandler } from 'astro'
import { defineMiddleware } from 'astro:middleware'
import { getSession } from 'auth-astro/server'

export const auth: MiddlewareHandler = defineMiddleware(
  async (context, next) => {
    let session = await getSession(context.request)
    //console.log(session)
    //console.log(context)
    if (
      !session &&
      !context.url.pathname.startsWith('/api') &&
      !context.url.pathname.startsWith('/login')
    ) {
      return context.redirect('/login')
    }
    if (session && context.url.pathname === '/login') {
      return context.redirect('/')
    }
    return next()
  },
)

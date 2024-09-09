import type {LoaderFunction} from "@remix-run/node";
import {authenticate} from "@/services/auth.server";

export const loader: LoaderFunction = async ({request, context}) => {
  const {auth} = authenticate(context);
  await auth.logout(request, {redirectTo: "/"});
};
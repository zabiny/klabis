import {
	Breadcrumb,
	BreadcrumbItem,
	BreadcrumbList,
	BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { CountrySelect } from "@/components/ui/country-select";
import {
	Form,
	FormControl,
	FormDatePicker,
	FormDescription,
	FormField,
	FormInput,
	FormLabel,
	FormMessage,
} from "@/components/ui/form";
import MultipleSelector from "@/components/ui/multi-select";
import { PhoneInput } from "@/components/ui/phone-input";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import { getClient } from "@/services/auth.server";
import { getTextareaProps, useForm } from "@conform-to/react";
import { parseWithZod } from "@conform-to/zod";
import { addYears, format } from "date-fns";
import { LoaderCircle } from "lucide-react";
import { useEffect } from "react";
import type { Country } from "react-phone-number-input/min";
import { redirect } from "react-router";
import { useLocation, useNavigation } from "react-router";
import { toast } from "sonner";
import validator from "validator";
import * as z from "zod";
import { Route } from "./+types/settings.[userId].[part]";

const schema = z.object({
	id: z.number(),
	// registrationNumber: z.string().regex(/[A-Z]{3}[0-9]{4}/, {
	//   message: 'registrační číslo není ve formátu ABC1234',
	// }),
	firstName: z.string(),
	lastName: z.string(),
	dateOfBirth: z.date().transform((date) => format(date, "yyyy-MM-dd")),
	birthCertificateNumber: z.string().regex(/^[0-9]{6}\/[0-9]{3,4}$/, {
		message: "rodné číslo není ve formátu YYMMDDD/CCCC",
	}),
	nationality: z.string().length(2),
	sex: z.enum(["male", "female"]),
	identityCard: z.object({
		number: z
			.string()
			.refine((value) => validator.isIdentityCard(value, "any"), {
				message: "neplatné číslo OP",
			})
			.optional(),
		expiryDate: z.date().optional(),
	}),
	address: z.object({
		streetAndNumber: z.string(),
		city: z.string(),
		postalCode: z.string(),
		country: z.string().length(2),
	}),
	contact: z.object({
		email: z.string().email(),
		phone: z.string(),
		note: z.string().optional(),
	}),
	guardians: z.array(
		z.object({
			firstName: z.string(),
			lastName: z.string(),
			contact: z.object({
				email: z.string().email(),
				phone: z.string(),
			}),
			note: z.string().optional(),
		}),
	),
	siCard: z.number().optional(),
	bankAccount: z
		.string()
		.refine(validator.isIBAN, {
			message: "účet není ve formátu IBAN",
		})
		.optional(),
	dietaryRestrictions: z.string().optional(),
	drivingLicence: z.array(z.enum(["B", "BE", "C", "D", ""])).optional(),
	medicCourse: z.preprocess(
		(value) => value === "on",
		z.boolean().default(false),
	),
});

export const drivingLicenceOptions = [
	{ label: "Auto", value: "B" },
	{ label: "Auto + přívěs", value: "BE" },
	{ label: "Kamion", value: "C" },
	{ label: "Autobus", value: "D" },
	{ label: "Taktór", value: "T" },
];

export async function loader({ request, context, params }: Route.LoaderArgs) {
	const client = await getClient({ request, context });
	const userId = +(params.userId ?? "");
	const { data, error } = await client.GET(
		"/members/{memberId}/editMemberInfoForm",
		{
			params: {
				path: { memberId: userId },
			},
		},
	);
	if (error) {
		return redirect("/404");
	}
	return { ...data, id: userId };
}

export async function action({ request, context }: Route.ActionArgs) {
	const formData = await request.formData();
	const submission = parseWithZod(formData, { schema });

	if (submission.status !== "success") {
		return { ok: false, error: submission.error };
	}
	console.log(submission.value);
	const client = await getClient({ request, context });
	const { data, error } = await client.PUT(
		"/members/{memberId}/editMemberInfoForm",
		{
			params: {
				path: { memberId: submission.value.id },
			},
			body: submission.value,
		},
	);
	return { ok: !error, error };
}

export default function SettingsId({
	loaderData,
	actionData,
}: Route.ComponentProps) {
	const user = loaderData;
	const navigation = useNavigation();
	const part = useLocation().pathname?.split("/")?.pop();
	const isSubmitting = navigation.state === "submitting";
	const lastAction = actionData;
	useEffect(() => {
		if (lastAction === undefined) return;
		if (lastAction.ok) {
			toast.success("Profil byl úspěšně uložen.");
		} else {
			toast.error("Něco se nepovedlo.");
		}
	}, [lastAction]);
	console.log(user);
	const [form, fields] = useForm({
		defaultValue: {
			...user,
			medicCourse: user?.medicCourse ? "on" : "",
		},
		// Reuse the validation logic on the client
		onValidate: ({ formData }) => {
			const val = parseWithZod(formData, { schema });
			// console.log(val);
			return val;
		},
		shouldValidate: "onBlur",
		shouldRevalidate: "onInput",
	});
	const sidebarNavItems = [
		{
			title: "Osobní údaje",
			href: "personal",
		},
		{
			title: "Adresa",
			href: "address",
		},
		{
			title: "Kontakt",
			href: "contact",
		},
		{
			title: "ORIS",
			href: "oris",
		},
		{
			title: "Volitelné",
			href: "optional",
		},
	];
	return (
		<div className="mx-10 space-y-6 pb-16">
			<Breadcrumb>
				<BreadcrumbList>
					<BreadcrumbItem> Nastavení </BreadcrumbItem>
					<BreadcrumbSeparator />
					<BreadcrumbItem>
						{" "}
						{user?.firstName} {user?.lastName}{" "}
					</BreadcrumbItem>
				</BreadcrumbList>
			</Breadcrumb>
			<div className="space-y-0.5">
				<h2 className="text-2xl font-bold tracking-tight">Nastavení</h2>
				<p className="text-muted-foreground">
					Manage your account settings and set e-mail preferences.
				</p>
			</div>
			<Separator className="my-6" />
			<div className="flex flex-col space-y-8 lg:flex-row lg:space-x-12 lg:space-y-0">
				<aside className="-mx-4 lg:w-1/5">
					<nav className="flex space-x-2 lg:flex-col lg:space-x-0 lg:space-y-1">
						{sidebarNavItems.map((item) => (
							<a
								key={item.title}
								href={item.href}
								className={cn(
									buttonVariants({ variant: "ghost" }),
									(part === "" && item.href === "personal") ||
										part === item.href
										? "bg-muted hover:bg-muted"
										: "hover:bg-transparent hover:underline",
									"justify-start",
								)}
							>
								{item.title}
							</a>
						))}
					</nav>
				</aside>
				<div className="flex-1 lg:max-w-2xl">
					<Form form={form}>
						<input type="hidden" name={"id"} value={user.id} />
						<div
							className="space-y-6"
							hidden={part !== "" && part !== "personal"}
						>
							<div>
								<h3 id="osobni-udaje" className="text-lg font-medium">
									Osobní údaje
								</h3>
								<p className="text-sm text-muted-foreground">
									Update your account settings. Set your preferred language and
									timezone.
								</p>
							</div>

							<Separator />
							<FormField field={fields.firstName}>
								<FormLabel>Jméno</FormLabel>
								<FormControl>
									<FormInput placeholder="Pepa" />
								</FormControl>
								<FormMessage />
							</FormField>
							<FormField field={fields.lastName}>
								<FormLabel>Příjmení</FormLabel>
								<FormControl>
									<FormInput placeholder="Racek" />
								</FormControl>
								<FormMessage />
							</FormField>
							<FormField
								field={fields.nationality}
								render={(control) => (
									<>
										<FormLabel>Národnost</FormLabel>
										<FormControl>
											<CountrySelect
												value={control.value as Country}
												onChange={control.change}
											/>
										</FormControl>
										<FormMessage />
									</>
								)}
							/>
							<FormField className="flex flex-col" field={fields.dateOfBirth}>
								<FormLabel>Datum narození</FormLabel>
								<FormDatePicker
									fromDate={addYears(new Date(), -150)}
									toDate={new Date()}
								/>
								<FormMessage />
							</FormField>
							<FormField field={fields.birthCertificateNumber}>
								<FormLabel>Rodné číslo</FormLabel>
								<FormControl>
									<FormInput placeholder="YYMMDDD/CCCC" />
								</FormControl>
								<FormDescription>Pouze pro občany ČR.</FormDescription>
								<FormMessage />
							</FormField>
							<FormField
								field={fields.sex}
								render={(control) => (
									<>
										<FormLabel>Pohlaví</FormLabel>
										<FormControl>
											<Select
												defaultValue={control.value as string}
												onValueChange={control.change}
											>
												<FormControl>
													<SelectTrigger>
														<SelectValue placeholder="Vyberte pohlaví" />
													</SelectTrigger>
												</FormControl>
												<SelectContent>
													<SelectItem value="female">Žena</SelectItem>
													<SelectItem value="male">Muž</SelectItem>
												</SelectContent>
											</Select>
										</FormControl>
										<FormMessage />
									</>
								)}
							/>

							<FormField field={fields.identityCard.getFieldset().number}>
								<FormLabel>Číslo OP</FormLabel>
								<FormControl>
									<FormInput placeholder="1234567890" />
								</FormControl>
								<FormMessage />
							</FormField>
							<FormField
								className="flex flex-col"
								field={fields.identityCard.getFieldset().expiryDate}
							>
								<FormLabel>Datum expirace OP</FormLabel>
								<FormDatePicker
									fromDate={new Date()}
									toDate={addYears(new Date(), 50)}
								/>
								<FormMessage />
							</FormField>
						</div>

						<div className="space-y-6" hidden={part !== "address"}>
							<div>
								<h3 id="adresa" className="text-lg font-medium">
									Adresa
								</h3>
								<p className="text-sm text-muted-foreground">
									Update your account settings. Set your preferred language and
									timezone.
								</p>
							</div>
							<Separator />
							<FormField field={fields.address.getFieldset().streetAndNumber}>
								<FormLabel>Ulice a číslo</FormLabel>
								<FormControl>
									<FormInput placeholder="Ulice 123" />
								</FormControl>
								<FormMessage />
							</FormField>
							<FormField field={fields.address.getFieldset().city}>
								<FormLabel>Město</FormLabel>
								<FormControl>
									<FormInput placeholder="Praha" />
								</FormControl>
								<FormMessage />
							</FormField>
							<FormField field={fields.address.getFieldset().postalCode}>
								<FormLabel>PSČ</FormLabel>
								<FormControl>
									<FormInput placeholder="12345" />
								</FormControl>
								<FormMessage />
							</FormField>
							<FormField
								field={fields.address.getFieldset().country}
								render={(control) => (
									<>
										<FormLabel>Země</FormLabel>
										<FormControl>
											<CountrySelect
												value={control.value as Country}
												onChange={control.change}
											/>
										</FormControl>
										<FormMessage />
									</>
								)}
							/>
						</div>
						<div className={"space-y-6"} hidden={part !== "contact"}>
							<div>
								<h3 id="kontakt" className="text-lg font-medium">
									Kontakt
								</h3>
								<p className="text-sm text-muted-foreground">
									Update your account settings. Set your preferred language and
									timezone.
								</p>
							</div>
							<Separator />
							<FormField field={fields.contact.getFieldset().email}>
								<FormLabel>Email</FormLabel>
								<FormControl>
									<FormInput placeholder="pepe.zdepa@email.cz" />
								</FormControl>
								<FormMessage />
							</FormField>
							<FormField
								field={fields.contact.getFieldset().phone}
								render={(control) => (
									<>
										<FormLabel>Telefon</FormLabel>
										<FormControl>
											<PhoneInput
												value={
													(control.value as string).startsWith("+")
														? (control.value as string)
														: `+420${control.value}`
												}
												onChange={control.change}
											/>
										</FormControl>
										<FormMessage />
									</>
								)}
							/>
							<FormField field={fields.contact.getFieldset().note}>
								<FormLabel>Poznámka</FormLabel>
								<FormControl>
									<FormInput placeholder="Poznámka..." />
								</FormControl>
								<FormMessage />
							</FormField>
							<div>
								<h3 className="text-lg font-medium">
									Kontakt na zákonné zástupce
								</h3>
								<p className="text-sm text-muted-foreground">
									Update your account settings. Set your preferred language and
									timezone.
								</p>
							</div>

							<ul>
								{fields.guardians.getFieldList().map((guardian, index) => {
									const guardianFields = guardian.getFieldset();
									return (
										<li key={guardian.key}>
											<Card>
												<CardContent className="space-y-6 my-6">
													<FormField field={guardianFields.firstName}>
														<FormLabel>Jméno</FormLabel>
														<FormControl>
															<FormInput placeholder="Pepa" />
														</FormControl>
														<FormMessage />
													</FormField>
													<FormField field={guardianFields.lastName}>
														<FormLabel>Příjmení</FormLabel>
														<FormControl>
															<FormInput placeholder="Racek" />
														</FormControl>
														<FormMessage />
													</FormField>
													<FormField
														field={guardianFields.contact.getFieldset().email}
													>
														<FormLabel>Email</FormLabel>
														<FormControl>
															<FormInput
																placeholder="pepe.zdepa@email.cz"
																type={"email"}
															/>
														</FormControl>
														<FormMessage />
													</FormField>
													<FormField
														field={guardianFields.contact.getFieldset().phone}
														render={(control) => (
															<>
																<FormLabel>Telefon</FormLabel>
																<FormControl>
																	<PhoneInput
																		value={
																			(control.value as string)?.startsWith("+")
																				? (control.value as string)
																				: `+420${control.value || ""}`
																		}
																		onChange={control.change}
																	/>
																</FormControl>
																<FormMessage />
															</>
														)}
													/>
													<FormField
														field={guardianFields.contact.getFieldset().note}
													>
														<FormLabel>Poznámka</FormLabel>
														<FormControl>
															<FormInput placeholder="Poznámka..." />
														</FormControl>
														<FormMessage />
													</FormField>
												</CardContent>
												<CardFooter className="justify-end">
													<Button
														{...form.remove.getButtonProps({
															name: fields.guardians.name,
															index,
														})}
													>
														Odstranit
													</Button>
												</CardFooter>
											</Card>
										</li>
									);
								})}
							</ul>
							<Button
								{...form.insert.getButtonProps({
									name: fields.guardians.name,
								})}
							>
								Přidat zástupce
							</Button>
						</div>
						<div className={"space-y-6"} hidden={part !== "oris"}>
							<div>
								<h3 className="text-lg font-medium">ORIS</h3>
								<p className="text-sm text-muted-foreground">
									Update your account settings. Set your preferred language and
									timezone.
								</p>
							</div>
							<Separator />
							<FormField field={fields.siCard}>
								<FormLabel>SI Čip</FormLabel>
								<FormControl>
									<FormInput placeholder="7200352" />
								</FormControl>
								<FormMessage />
							</FormField>
						</div>
						<div className={"space-y-6"} hidden={part !== "optional"}>
							<div>
								<h3 className="text-lg font-medium">Volitelné</h3>
								<p className="text-sm text-muted-foreground">
									Update your account settings. Set your preferred language and
									timezone.
								</p>
							</div>
							<Separator />
							<FormField field={fields.bankAccount}>
								<FormLabel>Bankovní účet IBAN</FormLabel>
								<FormControl>
									<FormInput placeholder="CZ1234567890" />
								</FormControl>
								<FormDescription>
									např. pro problácení účtů, cesťáků od oddílu
								</FormDescription>
								<FormMessage />
							</FormField>
							<FormField field={fields.dietaryRestrictions}>
								<FormLabel>Diety</FormLabel>
								<FormControl>
									<Textarea
										placeholder="Bezlepková, vegetariánská, vegan..."
										{...getTextareaProps(fields.dietaryRestrictions)}
									/>
								</FormControl>
								<FormMessage />
								<FormDescription>
									např. pro stravování na akcích
								</FormDescription>
							</FormField>
							<FormField
								field={fields.drivingLicence}
								render={(control) => (
									<>
										<FormLabel>Řidičský průkaz</FormLabel>
										<FormControl>
											<MultipleSelector
												options={drivingLicenceOptions}
												value={
													((control.value ?? []) as string[]).map(
														(value) =>
															drivingLicenceOptions.find(
																(option) => option.value === value,
															) ?? [],
													) as {
														label: string;
														value: string;
													}[]
												}
												onChange={(selectedValues) => {
													control.change(
														selectedValues.map((option) => option.value),
													);
												}}
											/>
										</FormControl>
										<FormMessage />
									</>
								)}
							/>
							<FormField
								field={fields.medicCourse}
								className="flex flex-row items-center justify-between rounded-lg border p-3 shadow-sm"
								render={(control) => (
									<>
										<div className="space-y-0.5">
											<FormLabel>Mám zravotnický kurz</FormLabel>
											<FormDescription>
												Pro potřeby akcí, kde je vyžadována zdravotní příprava
											</FormDescription>
										</div>
										<FormControl>
											<Switch
												checked={control.value === "on"}
												onCheckedChange={(checked) => {
													control.change(checked ? "on" : "");
												}}
											/>
										</FormControl>
									</>
								)}
							/>
						</div>
						<Separator className="my-6" />
						<Button type="submit" disabled={isSubmitting}>
							{isSubmitting && (
								<LoaderCircle className={"h-4 w-4 animate-spin"} />
							)}
							Uložit
						</Button>
					</Form>
				</div>
			</div>
		</div>
	);
}

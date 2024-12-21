import * as LabelPrimitive from "@radix-ui/react-label";
import { Slot } from "@radix-ui/react-slot";
import * as React from "react";

import { Button } from "@/components/ui/button";
import { Calendar, CalendarProps } from "@/components/ui/calendar";
import { Input, InputProps } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Popover,
	PopoverContent,
	PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import {
	FieldMetadata,
	FormMetadata,
	FormProvider,
	getFormProps,
	getInputProps,
	useField,
	useInputControl,
} from "@conform-to/react";
import { format } from "date-fns";
import { CalendarIcon } from "lucide-react";
import { SelectSingleEventHandler } from "react-day-picker";
import { Form as RemixForm } from "react-router";
import { FormProps as RemixFormProps } from "react-router";

const Form = <T extends Record<string, unknown>>({
	form,
	children,
	...props
}: RemixFormProps & { form: FormMetadata<T> }) => {
	return (
		<FormProvider context={form.context}>
			<RemixForm method="post" {...getFormProps(form)} {...props}>
				{children}
			</RemixForm>
		</FormProvider>
	);
};

const FormFieldContext = React.createContext<{ name: string }>(
	{} as { name: string },
);

const FormField = React.forwardRef<
	HTMLDivElement,
	React.HTMLAttributes<HTMLDivElement> & {
		field: FieldMetadata;
		render?: (control: ReturnType<typeof useInputControl>) => React.ReactNode;
	}
>(({ className, render, children, field, ...props }, ref) => {
	// @ts-ignore
	const control = useInputControl(field);
	return (
		<FormFieldContext.Provider value={{ name: field.name }}>
			<div ref={ref} className={cn("space-y-2", className)} {...props}>
				{render ? render(control) : children}
			</div>
		</FormFieldContext.Provider>
	);
});

const useFormField = () => {
	const fieldContext = React.useContext(FormFieldContext);
	const [meta] = useField(fieldContext.name);
	if (!fieldContext) {
		throw new Error("useFormField should be used within <FormField>");
	}

	return {
		formItemId: `${meta.id}-form-item`,
		formDescriptionId: `${meta.id}-form-item-description`,
		formMessageId: `${meta.id}-form-item-message`,
		valid: meta.valid,
		errors: meta.errors,
		dirty: meta.dirty,
		name: meta.name,
		meta: meta,
	};
};

const FormLabel = React.forwardRef<
	React.ElementRef<typeof LabelPrimitive.Root>,
	React.ComponentPropsWithoutRef<typeof LabelPrimitive.Root>
>(({ className, ...props }, ref) => {
	const field = useFormField();

	return (
		<Label
			ref={ref}
			className={cn(!field.valid && "text-destructive", className)}
			htmlFor={field.formItemId}
			{...props}
		/>
	);
});
FormLabel.displayName = "FormLabel";

const FormControl = React.forwardRef<
	React.ElementRef<typeof Slot>,
	React.ComponentPropsWithoutRef<typeof Slot>
>(({ ...props }, ref) => {
	const field = useFormField();

	return (
		<Slot
			ref={ref}
			id={field.formItemId}
			aria-describedby={
				field.valid
					? `${field.formDescriptionId}`
					: `${field.formDescriptionId} ${field.formMessageId}`
			}
			aria-invalid={!field.valid}
			{...props}
		/>
	);
});
FormControl.displayName = "FormControl";

const FormDescription = React.forwardRef<
	HTMLParagraphElement,
	React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => {
	const field = useFormField();

	return (
		<p
			ref={ref}
			id={field.formDescriptionId}
			className={cn("text-sm text-muted-foreground", className)}
			{...props}
		/>
	);
});
FormDescription.displayName = "FormDescription";

const FormMessage = React.forwardRef<
	HTMLParagraphElement,
	React.HTMLAttributes<HTMLParagraphElement>
>(({ className, children, ...props }, ref) => {
	const field = useFormField();
	const body = field.errors ? field.errors[0] : children;
	if (!body) {
		return null;
	}

	return (
		<p
			ref={ref}
			id={field.formMessageId}
			className={cn("text-sm font-medium text-destructive", className)}
			{...props}
		>
			{body}
		</p>
	);
});
FormMessage.displayName = "FormMessage";

const FormDatePicker = (props: CalendarProps) => {
	const field = useFormField();
	const control = useInputControl(field.meta as FieldMetadata<Date>);

	const dateToLocalTime = (date: Date): string => {
		return format(date, "yyyy-MM-dd");
		// const offset = date.getTimezoneOffset();
		// const localDate = new Date(date.getTime() - offset * 60 * 1000);
		// return localDate.toISOString().split("T")[0];
	};

	// @ts-ignore
	return (
		<Popover>
			<PopoverTrigger asChild>
				<FormControl>
					<Button
						variant={"outline"}
						className={cn(
							"w-[240px] pl-3 text-left font-normal",
							!control.value && "text-muted-foreground",
						)}
					>
						{control.value ? (
							format(new Date(control.value as string), "PPP")
						) : (
							<span>Vyberte datum</span>
						)}
						<CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
					</Button>
				</FormControl>
			</PopoverTrigger>
			<PopoverContent className="w-auto p-0" align="start">
				<Calendar
					mode="single"
					weekStartsOn={1}
					captionLayout="dropdown-buttons"
					{...props}
					selected={
						control.value ? new Date(control.value as string) : undefined
					}
					// @ts-ignore
					onSelect={(_day: Date | undefined, selectedDay) =>
						control.change(dateToLocalTime(selectedDay))
					}
					initialFocus
				/>
			</PopoverContent>
		</Popover>
	);
};
FormDatePicker.displayName = "FormDatePicker";

const FormInput = React.forwardRef<
	HTMLInputElement,
	InputProps & React.RefAttributes<HTMLInputElement>
>(({ className, children, type, ...props }, ref) => {
	const field = useFormField();

	return (
		<Input
			className={!field.valid ? "error" : ""}
			// @ts-ignore
			{...getInputProps(field.meta, { type: type ?? "text" })}
			{...props}
			key={field.name}
		/>
	);
});
FormInput.displayName = "FormInput";

export {
	Form,
	FormControl,
	FormDescription,
	FormField,
	FormDatePicker,
	FormLabel,
	FormInput,
	FormMessage,
	useFormField,
};

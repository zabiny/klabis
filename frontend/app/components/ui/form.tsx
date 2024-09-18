import * as LabelPrimitive from "@radix-ui/react-label";
import { Slot } from "@radix-ui/react-slot";
import * as React from "react";

import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import {
  FieldMetadata,
  FormMetadata,
  FormProvider,
  getFormProps,
  useField,
  useInputControl
} from "@conform-to/react";
import {Form as RemixForm } from "@remix-run/react";
import {RemixFormProps} from "@remix-run/react/dist/components";
import {Popover, PopoverContent, PopoverTrigger} from "@/components/ui/popover";
import {Button} from "@/components/ui/button";
import {format, formatISO, parseISO} from "date-fns";
import {CalendarIcon} from "lucide-react";
import {Calendar} from "@/components/ui/calendar";


const Form = <T extends Record<string, unknown>,>({ form, children, ...props }: RemixFormProps & { form: FormMetadata<T> }) => {
  return (
    <FormProvider context={form.context}>
      <RemixForm method="post" {...getFormProps(form)} {...props}>
        {children}
      </RemixForm>
    </FormProvider>
  );
}

const FormFieldContext = React.createContext<{name: string}>({} as {name: string});

const FormField = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement> & {field: FieldMetadata, render?: (control: ReturnType<typeof useInputControl>) => React.ReactNode}>(
  ({ className, render, children, field, ...props }, ref) => {
    // @ts-ignore
    const control = useInputControl(field);
  return (
    <FormFieldContext.Provider value={{ name: field.name }}>
      <div
        ref={ref}
        className={cn("space-y-2", className)}
        {...props} >
        { render ? render(control) : children }
      </div>
    </FormFieldContext.Provider>
  );
});

const useFormField = () => {
  const fieldContext = React.useContext(FormFieldContext);
  const [meta,] = useField(fieldContext.name);
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

const FormControl = React.forwardRef<React.ElementRef<typeof Slot>, React.ComponentPropsWithoutRef<typeof Slot>>(
  ({ ...props }, ref) => {
    const field = useFormField();

    return (
      <Slot
        ref={ref}
        id={field.formItemId}
        aria-describedby={field.valid ? `${field.formDescriptionId}` : `${field.formDescriptionId} ${field.formMessageId}`}
        aria-invalid={!field.valid}
        {...props}
      />
    );
  }
);
FormControl.displayName = "FormControl";

const FormDescription = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLParagraphElement>>(
  ({ className, ...props }, ref) => {
    const field = useFormField();

    return (
      <p
        ref={ref}
        id={field.formDescriptionId}
        className={cn("text-sm text-muted-foreground", className)}
        {...props}
      />
    );
  }
);
FormDescription.displayName = "FormDescription";

const FormMessage = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLParagraphElement>>(
  ({ className, children, ...props }, ref) => {
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
  }
);
FormMessage.displayName = "FormMessage";

const FormDatePicker = () => {
  const field = useFormField();
  const control = useInputControl(field.meta as FieldMetadata<Date>);

  const dateToLocalTime = (date: Date): string => {
    return format(date, 'yyyy-MM-dd');
    // const offset = date.getTimezoneOffset();
    // const localDate = new Date(date.getTime() - offset * 60 * 1000);
    // return localDate.toISOString().split("T")[0];
  };

  return (
  <Popover>
    <PopoverTrigger asChild>
      <FormControl>
        <Button
          variant={'outline'}
          className={cn(
            'w-[240px] pl-3 text-left font-normal',
            !control.value && 'text-muted-foreground',
          )}
        >
          {control.value ? (
            format(new Date(control.value as string), 'PPP')
            ) : (
              <span>Vyberte datum</span>
            )}
            <CalendarIcon className="ml-auto h-4 w-4 opacity-50"/>
          </Button>
        </FormControl>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          weekStartsOn={1}
          captionLayout="dropdown-buttons"
          fromYear={1900}
          toYear={new Date().getFullYear()}
          selected={
            control.value ? new Date(control.value as string) : undefined
          }
          onSelect={(value) => {
            return value ? control.change(dateToLocalTime(value as Date)) : undefined
          }
          }
          disabled={(date) =>
            date > new Date() || date < new Date('1900-01-01')
          }
          initialFocus
        />
      </PopoverContent>
    </Popover>
  );
};
FormDatePicker.displayName = "FormDatePicker";



export { Form, FormControl, FormDescription, FormField, FormDatePicker, FormLabel, FormMessage, useFormField };


import { Separator } from '@/components/ui/separator'
import * as z from "zod";
import {ActionFunctionArgs, redirect} from "@remix-run/server-runtime";
import {Input} from "@/components/ui/input";
import {json, useActionData, useLoaderData, useNavigation,} from "@remix-run/react";
import {Form, FormControl, FormDatePicker, FormDescription, FormField, FormLabel, FormMessage} from "@/components/ui/form";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {Button} from "@/components/ui/button";
import {format} from "date-fns";
import {LoaderCircle} from "lucide-react";
import {parseWithZod} from "@conform-to/zod";
import {getInputProps, getTextareaProps, useForm} from "@conform-to/react";
import {getClient} from "@/services/auth.server";
import {CountrySelect} from "@/components/ui/country-select";
import type {Country} from "react-phone-number-input/min";
import {Card, CardContent, CardHeader} from "@/components/ui/card";
import {PhoneInput} from "@/components/ui/phone-input";
import {toast} from "sonner";
import {useEffect} from "react";
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
import MultipleSelector from '@/components/ui/multi-select';
import validator from 'validator';

const schema = z.object({
  id: z.number(),
  // registrationNumber: z.string().regex(/[A-Z]{3}[0-9]{4}/, {
  //   message: 'registrační číslo není ve formátu ABC1234',
  // }),
  firstName: z.string(),
  lastName: z.string(),
  dateOfBirth: z.date().transform((date) => format(date, 'yyyy-MM-dd')),
  birthCertificateNumber: z.string().regex(/^[0-9]{6}\/[0-9]{3,4}$/, {
    message: 'rodné číslo není ve formátu YYMMDDD/CCCC',
  }),
  nationality: z.string().length(2),
  sex: z.enum(['male', 'female']),
  identityCard: z.object({
    number: z.string().refine((value) => validator.isIdentityCard(value, 'any'), {
      message: 'neplatné číslo OP',
    }).optional(),
    expiryDate: z.date().optional()
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
  bankAccount: z.string().refine(validator.isIBAN, {
    message: 'účet není ve formátu IBAN',
  }).optional(),
  dietaryRestrictions: z.string().optional(),
  drivingLicence: z.array(z.enum(['B', 'BE', 'C', 'D', ''])).optional(),
  medicCourse: z.preprocess((value) => value === "on", z.boolean().default(false)),
});

const drivingLicenceOptions = [
  { label: 'Auto', value: 'B' },
  { label: 'Auto + přívěs', value: 'BE' },
  { label: 'Kamion', value: 'C' },
  { label: 'Autobus', value: 'D' },
  { label: 'Taktór', value: 'T' },
];

export async function loader({ request, context, params }: ActionFunctionArgs) {
  const client = await getClient({request, context});
  const userId = +(params.userId ?? "");
  const {data, error} = await client.GET('/members/{memberId}/editMemberInfoForm', {
    params: {
      path: {memberId: userId}
    }
  });
  if (error) {
    return redirect('/404');
  }
  return json({...data, id: userId});
}

export async function action({ request, context }: ActionFunctionArgs) {
  const formData = await request.formData();
  const submission = parseWithZod(formData, { schema });

  if (submission.status !== 'success') {
    return json({ ok: false, error: submission.error});
  }
  console.log(submission.value);
  const client = await getClient({request, context});
  const {data, error} = await  client.PUT('/members/{memberId}/editMemberInfoForm', {
    params: {
      path: { memberId: submission.value.id }
    },
    body: submission.value
  })
  return json({ ok: !error, error });
}

export default function Profile() {
  const user = useLoaderData<typeof loader>();
  const navigation = useNavigation();
  const isSubmitting = navigation.state === 'submitting';
  const lastAction = useActionData<typeof action>();
  useEffect(() => {
    if (lastAction === undefined) return;
    if (lastAction.ok) {
      toast.success("Profil byl úspěšně uložen.");
    } else {
      toast.error("Něco se nepovedlo.");
    }
  }, [lastAction]);
  console.log(user)
  const [form, fields] = useForm({
    defaultValue: {
      ...user
    },
    // Reuse the validation logic on the client
    onValidate: ({ formData }) => {
      const val = parseWithZod(formData, { schema });
      // console.log(val);
      return val;
    },
    shouldValidate: "onBlur",
    shouldRevalidate: "onInput"
  });

  // @ts-ignore
  return <div className="space-y-6">
      <div>
        <h3 className="text-lg font-medium">Profil</h3>
        <p className="text-sm text-muted-foreground">
          Update your account settings. Set your preferred language and
          timezone.
        </p>
      </div>
      <Separator />

    <Form className="space-y-6" form={form}>
      <input type="hidden" name={"id"} value={user.id}/>
      <FormField field={fields.firstName}>
        <FormLabel>Jméno</FormLabel>
        <FormControl>
          <Input
            className={!fields.firstName.valid ? 'error' : ''}
            {...getInputProps(fields.firstName, {type: 'email'})}
            placeholder="Pepa"
          />
        </FormControl>
        <FormMessage/>
      </FormField>
      <FormField field={fields.lastName}>
        <FormLabel>Příjmení</FormLabel>
        <FormControl>
          <Input
            className={!fields.lastName.valid ? 'error' : ''}
            {...getInputProps(fields.lastName, {type: 'email'})}
            placeholder="Racek"
          />
        </FormControl>
        <FormMessage/>
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
            <FormMessage/>
          </>
        )}
      />
      <FormField className="flex flex-col" field={fields.dateOfBirth}>
        <FormLabel>Datum narození</FormLabel>
        <FormDatePicker/>
        <FormMessage/>
      </FormField>
      <FormField field={fields.birthCertificateNumber}>
        <FormLabel>Rodné číslo</FormLabel>
        <FormControl>
          <Input placeholder="YYMMDDD/CCCC" {...getInputProps(fields.birthCertificateNumber, {type: "text"})} />
        </FormControl>
        <FormDescription>Pouze pro občany ČR.</FormDescription>
        <FormMessage/>
      </FormField>
      <FormField
        field={fields.sex}
        render={(control) => <>
          <FormLabel>Pohlaví</FormLabel>
          <FormControl>
            <Select
              defaultValue={control.value as string}
              onValueChange={control.change}
            >
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="Vyberte pohlaví"/>
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem value="female">Žena</SelectItem>
                <SelectItem value="male">Muž</SelectItem>
              </SelectContent>
            </Select>
          </FormControl>
          <FormMessage/>
        </>
        }
      />
      <FormField field={fields.identityCard.getFieldset().number}>
        <FormLabel>Číslo OP</FormLabel>
        <FormControl>
          <Input placeholder="1234567890"
                 {...getInputProps(fields.identityCard.getFieldset().number, {type: 'text'})}
          />
        </FormControl>
        <FormMessage/>
      </FormField>
      <FormField className="flex flex-col" field={fields.identityCard.getFieldset().expiryDate}>
        <FormLabel>Datum expirace OP</FormLabel>
        <FormDatePicker/>
        <FormMessage/>
      </FormField>
      <Card>
        <CardHeader>Adresa</CardHeader>
        <CardContent className='space-y-6'>
          <FormField field={fields.address.getFieldset().streetAndNumber}>
            <FormLabel>Ulice a číslo</FormLabel>
            <FormControl>
              <Input
                placeholder="Ulice 123" {...getInputProps(fields.address.getFieldset().streetAndNumber, {type: "text"})} />
            </FormControl>
            <FormMessage/>
          </FormField>
          <FormField field={fields.address.getFieldset().city}>
            <FormLabel>Město</FormLabel>
            <FormControl>
              <Input placeholder="Praha" {...getInputProps(fields.address.getFieldset().city, {type: "text"})} />
            </FormControl>
            <FormMessage/>
          </FormField>
          <FormField field={fields.address.getFieldset().postalCode}>
            <FormLabel>PSČ</FormLabel>
            <FormControl>
              <Input placeholder="12345" {...getInputProps(fields.address.getFieldset().postalCode, {type: "text"})} />
            </FormControl>
            <FormMessage/>
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
                <FormMessage/>
              </>
            )}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>Kontakt</CardHeader>
        <CardContent className='space-y-6'>
          <FormField field={fields.contact.getFieldset().email}>
            <FormLabel>Email</FormLabel>
            <FormControl>
              <Input
                placeholder="pepe.zdepa@email.cz" {...getInputProps(fields.contact.getFieldset().email, {type: "email"})} />
            </FormControl>
            <FormMessage/>
          </FormField>
          <FormField
            field={fields.contact.getFieldset().phone}
            render={(control) => (
              <>
                <FormLabel>Telefon</FormLabel>
                <FormControl>
                  <PhoneInput
                    value={(control.value as string).startsWith("+") ? control.value as string : `+420${control.value}`}
                    onChange={control.change}/>
                </FormControl>
                <FormMessage/>
              </>
            )}
          />
          <FormField field={fields.contact.getFieldset().note}>
            <FormLabel>Poznámka</FormLabel>
            <FormControl>
              <Input placeholder="Poznámka..." {...getInputProps(fields.contact.getFieldset().note, {type: "text"})} />
            </FormControl>
            <FormMessage/>
          </FormField>
        </CardContent>
      </Card>
      <ul>
        {fields.guardians.getFieldList().map((guardian, index) => {
          const guardianFields = guardian.getFieldset();
          return <li key={guardian.key}>
            <Card>
              <CardHeader>Kontakt</CardHeader>
              <CardContent className='space-y-6'>
            <FormField field={guardianFields.firstName}>
              <FormLabel>Jméno</FormLabel>
              <FormControl>
                <Input
                  className={!guardianFields.firstName.valid ? 'error' : ''}
                  {...getInputProps(guardianFields.firstName, {type: 'email'})}
                  placeholder="Pepa"
                />
              </FormControl>
              <FormMessage/>
            </FormField>
            <FormField field={guardianFields.lastName}>
              <FormLabel>Příjmení</FormLabel>
              <FormControl>
                <Input
                  className={!guardianFields.lastName.valid ? 'error' : ''}
                  {...getInputProps(guardianFields.lastName, {type: 'email'})}
                  placeholder="Racek"
                />
              </FormControl>
              <FormMessage/>
            </FormField>
                <FormField field={guardianFields.contact.getFieldset().email}>
                  <FormLabel>Email</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="pepe.zdepa@email.cz" {...getInputProps(guardianFields.contact.getFieldset().email, {type: "email"})} />
                  </FormControl>
                  <FormMessage/>
                </FormField>
                {/*<FormField*/}
                {/*  field={guardianFields.contact.getFieldset().phone}*/}
                {/*  render={(control) => (*/}
                {/*    <>*/}
                {/*      <FormLabel>Telefon</FormLabel>*/}
                {/*      <FormControl>*/}
                {/*        <PhoneInput*/}
                {/*          value={(control.value as string).startsWith("+") ? control.value as string : `+420${control.value}`}*/}
                {/*          onChange={control.change}/>*/}
                {/*      </FormControl>*/}
                {/*      <FormMessage/>*/}
                {/*    </>*/}
                {/*  )}*/}
                {/*/>*/}
                <FormField field={guardianFields.contact.getFieldset().note}>
                  <FormLabel>Poznámka</FormLabel>
                  <FormControl>
                    <Input placeholder="Poznámka..." {...getInputProps(guardianFields.contact.getFieldset().note, {type: "text"})} />
                  </FormControl>
                  <FormMessage/>
                </FormField>
              </CardContent>
            </Card>
            <input name={guardian.name}/>
            <Button
                    {...form.reorder.getButtonProps({
                      name: fields.guardians.name,
                      from: index,
                      to: 0,
                    })}
            >
              Move to top
            </Button>
            <Button
                    {...form.remove.getButtonProps({
                      name: fields.guardians.name,
                      index,
                    })}
            >
              Delete
            </Button>
          </li>
        })}
      </ul>
      <Button
        {...form.insert.getButtonProps({
          name: fields.guardians.name,
        })}
      >
        Add task
      </Button>
      <FormField field={fields.siCard}>
        <FormLabel>SI Čip</FormLabel>
        <FormControl>
          <Input placeholder="7200352" {...getInputProps(fields.siCard, {type: 'text'})} />
        </FormControl>
        <FormMessage/>
      </FormField>
      <FormField
        field={fields.bankAccount}>
        <FormLabel>Bankovní účet IBAN</FormLabel>
        <FormControl>
          <Input placeholder="CZ1234567890" {...getInputProps(fields.bankAccount, {type: 'text'})} />
        </FormControl>
        <FormDescription>
          např. pro problácení účtů, cesťáků od oddílu
        </FormDescription>
        <FormMessage/>
      </FormField>
      <FormField field={fields.dietaryRestrictions}>
        <FormLabel>Diety</FormLabel>
        <FormControl>
          <Textarea
            placeholder="Bezlepková, vegetariánská, vegan..."
            {...getTextareaProps(fields.dietaryRestrictions)}
          />
        </FormControl>
        <FormMessage/>
        <FormDescription>např. pro stravování na akcích</FormDescription>
      </FormField>
      <FormField field={fields.drivingLicence}
                 render={(control) => <>
                   <FormLabel>Řidičský průkaz</FormLabel>
                   <FormControl>
                     <MultipleSelector
                       options={drivingLicenceOptions}
                       value={
                         ((control.value ?? []) as string[]).map((value) => drivingLicenceOptions.find((option) => option.value === value) ?? []) as {
                           label: string,
                           value: string
                         }[]}
                       onChange={(selectedValues) => {
                         control.change(selectedValues.map((option) => option.value));
                       }}
                     />
                   </FormControl>
                   <FormMessage/>
                 </>
                 }
      />
      <FormField
        field={fields.medicCourse}
        className="flex flex-row items-center justify-between rounded-lg border p-3 shadow-sm"
        render={(control) => <>
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
                control.change(checked ? "on" : "off");
              }}
            />
          </FormControl>
        </>
        }
      />

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting && <LoaderCircle className={'h-4 w-4 animate-spin'}/>}
        Uložit
      </Button>
    </Form>
  </div>;
}

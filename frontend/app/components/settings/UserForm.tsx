
import { zodResolver } from '@hookform/resolvers/zod'
import { CalendarIcon, CaretSortIcon, CheckIcon } from '@radix-ui/react-icons'
import { format } from 'date-fns'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import isIdentityCard from 'validator/lib/isIdentityCard'
import isIBAN from 'validator/lib/isIBAN'

import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Textarea } from '@/components/ui/textarea'
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
} from '@/components/ui/command'
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { toast } from '@/components/ui/use-toast'
import { CountrySelect } from '@/components/ui/country-select.tsx'
import type { Country } from 'react-phone-number-input'
import * as RPNInput from 'react-phone-number-input/min'
import { PhoneInput } from '@/components/ui/phone-input.tsx'
import { Card, CardContent, CardHeader } from '@/components/ui/card.tsx'
import {
  Select,
  SelectContent,
  SelectValue,
  SelectItem,
  SelectTrigger,
} from '@/components/ui/select.tsx'
import { DefaultContext } from 'react-icons'
import { Checkbox } from '@/components/ui/checkbox.tsx'
import { Switch } from '@/components/ui/switch.tsx'

const userFormSchema = z.object({
  registrationNumber: z.string().regex(/[A-Z]{3}[0-9]{4}/, {
    message: 'registrační číslo není ve formátu ABC1234',
  }),
  firstName: z.string(),
  lastName: z.string(),
  dateOfBirth: z.date(),
  birthCertificateNumber: z.string().regex(/[0-9]{6}\/[0-9]{4}/, {
    message: 'rodné číslo není ve formátu YYMMDDD/CCCC',
  }),
  identityCard: z.object({
    number: z.string().refine(isIdentityCard, {
      message: 'neplatné číslo OP',
    }),
    dateOfExpiry: z.date(),
  }),
  nationality: z.string().length(2),
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
  siCards: z.array(
    z.object({
      number: z.number(),
      isPrimary: z.boolean(),
    }),
  ),
  sex: z.enum(['male', 'female']),
  bankAccount: z.string().refine(isIBAN, {
    message: 'účet není ve formátu IBAN',
  }),
  dietaryRestrictions: z.string().optional(),
  drivingLicence: z.enum(['B', 'BE', 'C', 'D', '']).optional(),
  medicCourse: z.boolean().optional(),
})

type userFormValues = z.infer<typeof userFormSchema>

interface userFormProps {
  defaultValues?: userFormValues
}
export function UserForm({ defaultValues }: userFormProps) {
  const form = useForm<userFormValues>({
    resolver: zodResolver(userFormSchema),
    defaultValues,
  })

  function onSubmit(data: userFormValues) {
    toast({
      title: 'You submitted the following values:',
      description: (
        <pre className="mt-2 w-[340px] rounded-md bg-slate-950 p-4">
          <code className="text-white">{JSON.stringify(data, null, 2)}</code>
        </pre>
      ),
    })
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
        <FormField
          control={form.control}
          name="registrationNumber"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Registrační číslo</FormLabel>
              <FormControl>
                <Input placeholder="ZBMxxxx" {...field} disabled />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="firstName"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Jméno</FormLabel>
              <FormControl>
                <Input placeholder="Pepa" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="lastName"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Příjmení</FormLabel>
              <FormControl>
                <Input placeholder="Racek" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="dateOfBirth"
          render={({ field }) => (
            <FormItem className="flex flex-col">
              <FormLabel>Datum narození</FormLabel>
              <Popover>
                <PopoverTrigger asChild>
                  <FormControl>
                    <Button
                      variant={'outline'}
                      className={cn(
                        'w-[240px] pl-3 text-left font-normal',
                        !field.value && 'text-muted-foreground',
                      )}
                    >
                      {field.value ? (
                        format(field.value, 'PPP')
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
                    captionLayout="dropdown-buttons"
                    fromYear={1900}
                    toYear={new Date().getFullYear()}
                    selected={field.value}
                    onSelect={field.onChange}
                    disabled={(date) =>
                      date > new Date() || date < new Date('1900-01-01')
                    }
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="birthCertificateNumber"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Rodné číslo</FormLabel>
              <FormControl>
                <Input placeholder="YYMMDDD/CCCC" {...field} />
              </FormControl>
              <FormDescription>Pouze pro občany ČR.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="sex"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Pohlaví</FormLabel>
              <FormControl>
                <Select
                  defaultValue={field.value}
                  onValueChange={field.onChange}
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
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="identityCard.number"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Číslo OP</FormLabel>
              <FormControl>
                <Input placeholder="1234567890" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="identityCard.dateOfExpiry"
          render={({ field }) => (
            <FormItem className="flex flex-col">
              <FormLabel>Datum expirace OP</FormLabel>
              <Popover>
                <PopoverTrigger asChild>
                  <FormControl>
                    <Button
                      variant={'outline'}
                      className={cn(
                        'w-[240px] pl-3 text-left font-normal',
                        !field.value && 'text-muted-foreground',
                      )}
                    >
                      {field.value ? (
                        format(field.value, 'PPP')
                      ) : (
                        <span>Pick a date</span>
                      )}
                      <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                    </Button>
                  </FormControl>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="start">
                  <Calendar
                    mode="single"
                    captionLayout="dropdown-buttons"
                    fromYear={new Date().getFullYear()}
                    toYear={new Date().getFullYear() + 20}
                    selected={field.value}
                    onSelect={field.onChange}
                    disabled={(date) => date < new Date()}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="nationality"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Národnost</FormLabel>
              <FormControl>
                <CountrySelect
                  value={field.value as Country}
                  onChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Card>
          <CardHeader>Adresa</CardHeader>
          <CardContent>
            <FormField
              control={form.control}
              name="address.streetAndNumber"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Ulice a číslo</FormLabel>
                  <FormControl>
                    <Input placeholder="Ulice 123" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="address.city"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Město</FormLabel>
                  <FormControl>
                    <Input placeholder="Praha" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="address.postalCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>PSČ</FormLabel>
                  <FormControl>
                    <Input placeholder="12345" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="address.country"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Země</FormLabel>
                  <FormControl>
                    <CountrySelect
                      value={field.value as Country}
                      onChange={field.onChange}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>Kontakt</CardHeader>
          <CardContent>
            <FormField
              control={form.control}
              name="contact.email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email</FormLabel>
                  <FormControl>
                    <Input placeholder="pepe.zdepa@email.cz" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="contact.phone"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Telefon</FormLabel>
                  <FormControl>
                    <PhoneInput value={field.value} onChange={field.onChange} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="contact.note"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Poznámka</FormLabel>
                  <FormControl>
                    <Input placeholder="Poznámka..." {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>
        <FormField
          control={form.control}
          name="bankAccount"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Bankovní účet IBAN</FormLabel>
              <FormControl>
                <Input placeholder="CZ1234567890" {...field} />
              </FormControl>
              <FormDescription>
                např. pro problácení účtů, cesťáků od oddílu
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="dietaryRestrictions"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Diety</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="Bezlepková, vegetariánská, vegan..."
                  {...field}
                />
              </FormControl>
              <FormMessage />
              <FormDescription>např. pro stravování na akcích</FormDescription>
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="drivingLicence"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Řidičský průkaz</FormLabel>
              <FormControl>
                <Select
                  defaultValue={field.value}
                  onValueChange={field.onChange}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Vyberte řidičský průkaz" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value={null as unknown as string}>
                      Žádný
                    </SelectItem>
                    <SelectItem value="B">Auto</SelectItem>
                    <SelectItem value="BE">Auto + přívěs</SelectItem>
                    <SelectItem value="C">Kamion</SelectItem>
                    <SelectItem value="D">Autobus</SelectItem>
                    <SelectItem value="T">Taktór</SelectItem>
                  </SelectContent>
                </Select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="medicCourse"
          render={({ field }) => (
            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3 shadow-sm">
              <div className="space-y-0.5">
                <FormLabel>Mám zravotnický kurz</FormLabel>
                <FormDescription>
                  Pro potřeby akcí, kde je vyžadována zdravotní příprava
                </FormDescription>
              </div>
              <FormControl>
                <Switch
                  checked={field.value}
                  onCheckedChange={field.onChange}
                />
              </FormControl>
            </FormItem>
          )}
        />

        {/*<FormField*/}
        {/*  control={form.control}*/}
        {/*  name="language"*/}
        {/*  render={({ field }) => (*/}
        {/*    <FormItem className="flex flex-col">*/}
        {/*      <FormLabel>Language</FormLabel>*/}
        {/*      <Popover>*/}
        {/*        <PopoverTrigger asChild>*/}
        {/*          <FormControl>*/}
        {/*            <Button*/}
        {/*              variant="outline"*/}
        {/*              role="combobox"*/}
        {/*              className={cn(*/}
        {/*                'w-[200px] justify-between',*/}
        {/*                !field.value && 'text-muted-foreground',*/}
        {/*              )}*/}
        {/*            >*/}
        {/*              {field.value*/}
        {/*                ? languages.find(*/}
        {/*                    (language) => language.value === field.value,*/}
        {/*                  )?.label*/}
        {/*                : 'Select language'}*/}
        {/*              <CaretSortIcon className="ml-2 h-4 w-4 shrink-0 opacity-50" />*/}
        {/*            </Button>*/}
        {/*          </FormControl>*/}
        {/*        </PopoverTrigger>*/}
        {/*        <PopoverContent className="w-[200px] p-0">*/}
        {/*          <Command>*/}
        {/*            <CommandInput placeholder="Search language..." />*/}
        {/*            <CommandEmpty>No language found.</CommandEmpty>*/}
        {/*            <CommandGroup>*/}
        {/*              {languages.map((language) => (*/}
        {/*                <CommandItem*/}
        {/*                  value={language.label}*/}
        {/*                  key={language.value}*/}
        {/*                  onSelect={() => {*/}
        {/*                    form.setValue('language', language.value)*/}
        {/*                  }}*/}
        {/*                >*/}
        {/*                  <CheckIcon*/}
        {/*                    className={cn(*/}
        {/*                      'mr-2 h-4 w-4',*/}
        {/*                      language.value === field.value*/}
        {/*                        ? 'opacity-100'*/}
        {/*                        : 'opacity-0',*/}
        {/*                    )}*/}
        {/*                  />*/}
        {/*                  {language.label}*/}
        {/*                </CommandItem>*/}
        {/*              ))}*/}
        {/*            </CommandGroup>*/}
        {/*          </Command>*/}
        {/*        </PopoverContent>*/}
        {/*      </Popover>*/}
        {/*      <FormDescription>*/}
        {/*        This is the language that will be used in the dashboard.*/}
        {/*      </FormDescription>*/}
        {/*      <FormMessage />*/}
        {/*    </FormItem>*/}
        {/*  )}*/}
        {/*/>*/}
        <Button type="submit">Update account</Button>
      </form>
    </Form>
  )
}

import { ChevronDown } from "lucide-react";

import * as React from "react";

import * as RPNInput from "react-phone-number-input";

import flags from "react-phone-number-input/flags";

import cz from "react-phone-number-input/locale/cz.json";

import { Button } from "@/components/ui/button";
import {
	Command,
	CommandEmpty,
	CommandGroup,
	CommandInput,
	CommandItem,
	CommandList,
} from "@/components/ui/command";
import {
	Popover,
	PopoverContent,
	PopoverTrigger,
} from "@/components/ui/popover";

import { cn } from "@/lib/utils";

type CountrySelectOption = { label: string; value: RPNInput.Country };

type CountrySelectProps = {
	disabled?: boolean;
	value: RPNInput.Country;
	onChange: (value: RPNInput.Country) => void;
	phoneInput?: boolean;
};

export const CountrySelect = ({
	disabled,
	value,
	onChange,
	phoneInput,
}: CountrySelectProps) => {
	const handleSelect = React.useCallback(
		(country: RPNInput.Country) => {
			onChange(country);
		},
		[onChange],
	);

	return (
		<Popover>
			<PopoverTrigger asChild>
				<Button
					type="button"
					variant={"outline"}
					className={cn("flex gap-1 px-3")}
					disabled={disabled}
				>
					{value ? (
						<>
							<FlagComponent country={value} countryName={value} />
							{phoneInput || cz[value] || value}
						</>
					) : (
						"Vyberte zemi"
					)}
					{phoneInput && (
						<span className="text-sm text-foreground/50">
							{`+${RPNInput.getCountryCallingCode(value)}`}
						</span>
					)}
					<ChevronDown
						className={cn(
							"-mr-2 ml-2 h-4 w-4 opacity-50",
							disabled ? "hidden" : "opacity-100",
						)}
					/>
				</Button>
			</PopoverTrigger>
			<PopoverContent className="w-[300px] p-0">
				<Command
					filter={(value, search) => {
						const normValue = value
							.normalize("NFD")
							.replace(/[\u0300-\u036f]/g, "");
						const normSearch = search
							.normalize("NFD")
							.replace(/[\u0300-\u036f]/g, "");
						return normValue.toLowerCase().includes(normSearch.toLowerCase())
							? 1
							: 0;
					}}
				>
					<CommandList>
						<CommandInput placeholder="Search country..." />
						<CommandEmpty>No country found.</CommandEmpty>
						<CommandGroup>
							{RPNInput.getCountries().map((option) => (
								<CommandItem
									className="gap-2"
									key={option}
									onSelect={() => handleSelect(option)}
								>
									<FlagComponent
										country={option}
										countryName={cz[option] || option}
									/>
									<span className="flex-1 text-sm">{cz[option]}</span>
								</CommandItem>
							))}
						</CommandGroup>
					</CommandList>
				</Command>
			</PopoverContent>
		</Popover>
	);
};

export const FlagComponent = ({ country, countryName }: RPNInput.FlagProps) => {
	const Flag = flags[country];

	return (
		<span className="flex h-4 w-6 overflow-hidden rounded-sm bg-foreground/20">
			{Flag && <Flag title={countryName} />}
		</span>
	);
};
FlagComponent.displayName = "FlagComponent";

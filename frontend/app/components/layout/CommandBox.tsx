import {
	EnvelopeClosedIcon,
	GearIcon,
	PersonIcon,
} from "@radix-ui/react-icons";
import * as React from "react";

import {
	Command,
	CommandEmpty,
	CommandGroup,
	CommandInput,
	CommandItem,
	CommandList,
	CommandSeparator,
	CommandShortcut,
} from "@/components/ui/command";
import {
	Popover,
	PopoverContent,
	PopoverTrigger,
} from "@/components/ui/popover";
import { CalendarIcon } from "lucide-react";

export function CommandBox() {
	const [search, setSearch] = React.useState("");
	const [open, setOpen] = React.useState(false);

	return (
		<Popover open={open} onOpenChange={setOpen}>
			<Command className="w-auto border md:w-[400px]">
				<PopoverTrigger asChild>
					<CommandInput
						onBlur={() => {
							setOpen(false);
							setTimeout(() => setSearch(""), 100);
						}}
						value={search}
						onValueChange={setSearch}
						placeholder="Search ..."
						className="h-9"
					/>
				</PopoverTrigger>
				<PopoverContent
					forceMount={true}
					className="p-0 md:w-[365px]"
					onOpenAutoFocus={(e) => e.preventDefault()}
					onCloseAutoFocus={(e) => e.preventDefault()}
					align="start"
					hideWhenDetached={true}
					asChild
				>
					<CommandList>
						<CommandEmpty>No results found.</CommandEmpty>
						<CommandGroup heading="Events">
							<CommandItem>
								<CalendarIcon className="mr-2 h-4 w-4" />
								<span>Create new</span>
							</CommandItem>
						</CommandGroup>
						<CommandSeparator />
						<CommandGroup heading="Settings">
							<CommandItem>
								<PersonIcon className="mr-2 h-4 w-4" />
								<span>Profile</span>
								<CommandShortcut>⌘P</CommandShortcut>
							</CommandItem>
							<CommandItem>
								<EnvelopeClosedIcon className="mr-2 h-4 w-4" />
								<span>Mail</span>
								<CommandShortcut>⌘B</CommandShortcut>
							</CommandItem>
							<CommandItem>
								<GearIcon className="mr-2 h-4 w-4" />
								<span>Settings</span>
								<CommandShortcut>⌘S</CommandShortcut>
							</CommandItem>
						</CommandGroup>
						<CommandGroup heading="Groups">
							<CommandItem>
								<CalendarIcon className="mr-2 h-4 w-4" />
								<span>Dorost+</span>
							</CommandItem>
						</CommandGroup>
					</CommandList>
				</PopoverContent>
			</Command>
		</Popover>
	);
}

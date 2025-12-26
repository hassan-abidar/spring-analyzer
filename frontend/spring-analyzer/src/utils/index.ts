export const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleString();
};

export const classNames = (...classes: (string | boolean | undefined)[]): string => {
  return classes.filter(Boolean).join(' ');
};

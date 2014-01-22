function [n,l,f,r]=script()

here = fileparts(mfilename('fullpath'));

filename = fullfile(fileparts(here),'out','xopt');
filename = 'xopt';
[n,l,f,r]=plot_xopt(filename);
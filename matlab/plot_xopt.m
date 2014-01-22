function [n,l,f,r]=plot_xopt(scriptname)

eval(['[n,l,f,r]=' scriptname '();'])

figure

subplot(411)
plot(n)
subplot(412)
plot(f)
subplot(413)
plot(l)
subplot(414)
plot(r)